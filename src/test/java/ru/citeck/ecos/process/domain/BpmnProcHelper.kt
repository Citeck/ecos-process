package ru.citeck.ecos.process.domain

import com.hazelcast.core.HazelcastInstance
import jakarta.annotation.PostConstruct
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.rest.dto.migration.MigrationPlanGenerationDto
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.BpmnEventSubscriptionService
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID_WITH_APP
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnDurationKpiTimeType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_DEF_RECORDS_SOURCE_ID
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDefActions
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSyncService
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer.Companion.TASK_DOCUMENT_TYPE_ATT_PREFIX
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_SOURCE_ID
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.EcosWebAppApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets

private val typeRef = ModelUtils.getTypeRef("type0")

@Component
class BpmnProcHelper(
    val recordsService: RecordsService,
    val procDefRepo: ProcDefRepository,
    val procDefRevRepo: ProcDefRevRepository,
    val camundaRepositoryService: RepositoryService,
    val taskService: TaskService,
    val procTaskAttsSyncService: ProcTaskAttsSyncService,
    val hazelCast: HazelcastInstance,
    val eventsService: EventsService,
    val webAppApi: EcosWebAppApi,
    val bpmnEventSubscriptionService: BpmnEventSubscriptionService
) {

    private lateinit var stdListenerIds: Set<String>

    @PostConstruct
    fun init() {
        val listeners = HashSet<String>()
        webAppApi.doBeforeAppReady {
            eventsService.getListeners().forEach {
                it.value.listeners.forEach { listener ->
                    listeners.add(listener.config.id)
                }
            }
        }
        stdListenerIds = listeners
    }

    fun getBpmnProcessDefDto(resource: String, id: String): NewProcessDefDto {
        return getBpmnProcessDefDto(resource, IdInWs.create(id))
    }

    fun getBpmnProcessDefDto(resource: String, idInWs: IdInWs): NewProcessDefDto {
        return NewProcessDefDto(
            idInWs.id,
            MLText.EMPTY,
            BPMN_PROC_TYPE,
            idInWs.workspace,
            "xml",
            "{http://www.citeck.ru/model/content/idocs/1.0}type",
            typeRef,
            EntityRef.EMPTY,
            EntityRef.EMPTY,
            ResourceUtils.getFile("classpath:$resource")
                .readText(StandardCharsets.UTF_8)
                .toByteArray(StandardCharsets.UTF_8),
            null,
            enabled = true,
            autoStartEnabled = false,
            sectionRef = EntityRef.EMPTY
        )
    }

    fun saveAndDeployBpmnFromResource(resource: String, id: String) {
        saveAndDeployBpmnFromString(
            ResourceUtils.getFile("classpath:$resource")
                .readText(StandardCharsets.UTF_8),
            id
        )
    }

    fun saveBpmnWithAction(resource: String, id: String, action: BpmnProcessDefActions?) {
        val recordAtts = RecordAtts(EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, "")).apply {
            this["processDefId"] = id
            this["definition"] = ResourceUtils.getFile("classpath:$resource")
                .readText(StandardCharsets.UTF_8)

            action?.let {
                this["action"] = it.name
            }
        }

        recordsService.mutate(recordAtts)
    }

    fun createDurationKpiSettings(
        id: String,
        kpiType: BpmnKpiType = BpmnKpiType.DURATION,
        process: EntityRef,
        source: String? = null,
        sourceEventType: BpmnKpiEventType? = null,
        target: String,
        targetEventType: BpmnKpiEventType,
        timeType: BpmnDurationKpiTimeType = BpmnDurationKpiTimeType.CALENDAR,
        dmnCondition: EntityRef = EntityRef.EMPTY
    ): EntityRef {
        val kpiSettings = mapOf(
            "id" to id,
            "name" to "test kpi",
            "kpiType" to kpiType.name,
            "processRef" to process,
            "enabled" to true,
            "sourceBpmnActivityId" to source,
            "sourceBpmnActivityEvent" to sourceEventType?.name,
            "targetBpmnActivityId" to target,
            "targetBpmnActivityEvent" to targetEventType.name,
            "durationKpi" to "15m",
            "durationKpiTimeType" to timeType.name,
            "dmnCondition" to dmnCondition
        )

        return recordsService.create(BPMN_KPI_SETTINGS_SOURCE_ID_WITH_APP, kpiSettings)
    }

    fun createAttsSync(
        id: String,
        enabled: Boolean,
        source: TaskAttsSyncSource,
        name: String,
        attributesSync: List<TaskSyncAttribute>
    ): EntityRef {
        val attsSync = mapOf(
            "id" to id,
            "enabled" to enabled,
            "source" to source,
            "name" to name,
            "typeRef" to typeRef,
            "attributesSync" to attributesSync
        )

        return recordsService.create(PROC_TASK_ATTS_SYNC_SOURCE_ID, attsSync)
    }

    fun uploadNewVersionFromResource(resource: String, id: String, comment: String, replace: Pair<String, String>) {
        val content = ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)
        uploadNewVersion(content, id, comment, replace)
    }

    fun uploadNewVersion(content: String, id: String, comment: String, replace: Pair<String, String>) {
        val recordAtts = RecordAtts(EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, id)).apply {
            // Does not matter, we check field is not empty and increase major version
            this["version:version"] = "someVersion"
            this["version:comment"] = comment
            this["_content"] = content.replace(replace.first, replace.second)
        }

        recordsService.mutate(recordAtts)
    }

    fun copyBpmnModule(id: String, moduleId: String) {
        val recordAtts = RecordAtts(EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, id)).apply {
            this["moduleId"] = moduleId
        }

        recordsService.mutate(recordAtts)
    }

    fun saveBpmnWithActionAndReplaceDefinition(
        resource: String,
        id: String,
        action: BpmnProcessDefActions?,
        replace: Pair<String, String>
    ) {
        val recordAtts = RecordAtts(EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, "")).apply {
            this["processDefId"] = id
            this["definition"] = ResourceUtils.getFile("classpath:$resource")
                .readText(StandardCharsets.UTF_8)
                .replace(replace.first, replace.second)

            action?.let {
                this["action"] = it.name
            }
        }

        recordsService.mutate(recordAtts)
    }

    fun saveAndDeployBpmnFromString(bpmnData: String, id: String, workspace: String = "") {
        val recordAtts = RecordAtts(EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, "")).apply {
            this["processDefId"] = id
            this["definition"] = bpmnData
            this["action"] = BpmnProcessDefActions.DEPLOY.toString()
            if (workspace.isNotBlank()) {
                this[RecordConstants.ATT_WORKSPACE] = workspace
            }
        }

        recordsService.mutate(recordAtts)
    }

    fun saveAndDeployDmnFromResource(resource: String, id: String, workspace: String = "") {
        saveAndDeployDmnFromString(
            ResourceUtils.getFile("classpath:$resource")
                .readText(StandardCharsets.UTF_8),
            id,
            workspace
        )
    }

    fun saveAndDeployDmnFromString(dmnData: String, id: String, workspace: String = "") {
        val recordAtts = RecordAtts(EntityRef.create(AppName.EPROC, DMN_DEF_RECORDS_SOURCE_ID, "")).apply {
            this["defId"] = id
            this["definition"] = dmnData
            this["action"] = DmnDefActions.DEPLOY.toString()
            if (workspace.isNotBlank()) {
                this[RecordConstants.ATT_WORKSPACE] = workspace
            }
        }
        recordsService.mutate(recordAtts)
    }

    fun saveAndDeployBpmn(elementFolder: String, id: String) {
        saveAndDeployBpmnFromResource(
            "test/bpmn/elements/$elementFolder/$id.bpmn.xml",
            id
        )
    }

    fun cleanDefinitions() {
        procDefRevRepo.deleteAll()
        procDefRepo.deleteAll()
    }

    fun clearTasks() {
        taskService.createTaskQuery().list().forEach {
            taskService.deleteTask(it.id, true)
        }
    }

    fun cleanDeployments() {
        camundaRepositoryService.createDeploymentQuery().list().forEach {
            camundaRepositoryService.deleteDeployment(it.id, true, true, true)
        }
    }

    fun cleanTaskAttsSyncSettings() {
        recordsService.query(
            RecordsQuery.create {
                withSourceId(PROC_TASK_ATTS_SYNC_SOURCE_ID)
            }
        ).getRecords()
            .forEach {
                procTaskAttsSyncService.removeSyncSettings(it)
            }
    }

    fun fullCleanEventSubscriptions() {
        cleanDeployments()
        cleanDefinitions()

        bpmnEventSubscriptionService.clean()

        eventsService.getListeners().forEach {
            it.value.listeners.forEach { listener ->
                if (!stdListenerIds.contains(listener.config.id)) {
                    eventsService.removeListener(listener.config.id)
                }
            }
        }

        val cache = hazelCast.getMap<String, EventSubscription>(
            BPMN_EVENT_SUBSCRIPTIONS_BY_DEPLOYMENT_ID_CACHE_NAME
        )
        cache.clear()
    }

    fun queryLatestProcessDefEngineRecords(forUser: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnProcessDefEngineRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("onlyLatestVersion", true)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryBpmnVariableInstances(forUser: String, processInstanceId: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnVariableInstanceRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("processInstance", processInstanceId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryCalledProcessInstancesKeys(forUser: String, processInstanceId: String): List<String> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnCalledProcessInstanceRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnProcess", processInstanceId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                },
                mapOf("key" to "calledProcess.key")
            )
                .getRecords()
                .map {
                    it["key"].asText()
                }
        }
    }

    fun queryExternalTasks(forUser: String, processInstanceId: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnExternalTaskRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnProcess", processInstanceId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryIncidentsForProcessInstance(forUser: String, processInstanceId: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnIncidentRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnProcess", processInstanceId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryIncidentsForBpmnDefEngine(forUser: String, processDefEngineId: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnIncidentRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnDefEngine", processDefEngineId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryBpmnJobs(forUser: String, processInstanceId: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnJobRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnProcess", processInstanceId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryBpmnJobDefs(forUser: String, processDefEngineId: String): List<EntityRef> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnJobDefRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnDefEngine", processDefEngineId)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    fun queryMigrationPlan(
        forUser: String,
        sourceProcessDefinitionId: String,
        targetProcessDefinitionId: String
    ): List<BpmnProcessMigrationRecords.ProcessMigrationRecord> {
        return AuthContext.runAs(forUser) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnProcessMigrationRecords.ID)
                    withQuery(
                        BpmnProcessMigrationRecords.BpmnProcessMigrationQuery(
                            MigrationPlanGenerationDto().apply {
                                this.sourceProcessDefinitionId = sourceProcessDefinitionId
                                this.targetProcessDefinitionId = targetProcessDefinitionId
                            }
                        )
                    )
                },
                BpmnProcessMigrationRecords.ProcessMigrationRecord::class.java
            ).getRecords()
        }
    }

    fun queryTasks(predicate: Predicate, sortBy: SortBy? = null): List<EntityRef> {
        return recordsService.query(
            RecordsQuery.create {
                withSourceId(ProcTaskRecords.ID)
                withLanguage(PredicateService.LANGUAGE_PREDICATE)
                withQuery(predicate)
                withSortBy(sortBy)
                withPage(QueryPage(10_000, 0, null))
            }
        ).getRecords()
    }
}

fun String.withDocPrefix() = TASK_DOCUMENT_ATT_PREFIX + this
fun String.withDocTypePrefix() = TASK_DOCUMENT_TYPE_ATT_PREFIX + this
