package ru.citeck.ecos.process.domain

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.rest.dto.migration.MigrationPlanGenerationDto
import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.*
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnDurationKpiTimeType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.bpmn.kpi.config.BpmnKpiSettingsDaoConfig
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_DEF_RECORDS_SOURCE_ID
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDefActions
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct

private val typeRef = TypeUtils.getTypeRef("type0")

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnProcHelper(
    val recordsService: RecordsService,
    val procDefRepo: ProcDefRepository,
    val procDefRevRepo: ProcDefRevRepository,
    val camundaRepositoryService: RepositoryService
) {
    @PostConstruct
    private fun init() {
        helper = this
    }
}

private lateinit var helper: BpmnProcHelper

fun getBpmnProcessDefDto(resource: String, id: String): NewProcessDefDto {
    return NewProcessDefDto(
        id,
        MLText.EMPTY,
        BPMN_PROC_TYPE,
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

    helper.recordsService.mutate(recordAtts)
}

fun createDurationKpiSettings(
    id: String,
    kpiType: BpmnKpiType = BpmnKpiType.DURATION,
    process: String,
    source: String? = null,
    sourceEventType: BpmnKpiEventType? = null,
    target: String,
    targetEventType: BpmnKpiEventType,
    timeType: BpmnDurationKpiTimeType = BpmnDurationKpiTimeType.CALENDAR
): EntityRef {
    val kpiSettings = mapOf(
        "id" to id,
        "name" to "test kpi",
        "kpiType" to kpiType.name,
        "processRef" to EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, process),
        "enabled" to true,
        "sourceBpmnActivityId" to source,
        "sourceBpmnActivityEvent" to sourceEventType?.name,
        "targetBpmnActivityId" to target,
        "targetBpmnActivityEvent" to targetEventType.name,
        "durationKpi" to 10_000,
        "durationKpiTimeType" to timeType.name
    )

    return helper.recordsService.create("eproc/${BpmnKpiSettingsDaoConfig.SOURCE_ID}", kpiSettings)
}

fun uploadNewVersionFromResource(resource: String, id: String, comment: String, replace: Pair<String, String>) {
    val content = ResourceUtils.getFile("classpath:$resource")
        .readText(StandardCharsets.UTF_8)
    uploadNewVersion(content, id, comment, replace)
}

fun uploadNewVersion(content: String, id: String, comment: String, replace: Pair<String, String>) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, id)).apply {
        // Does not matter, we check field is not empty and increase major version
        this["version:version"] = "someVersion"
        this["version:comment"] = comment
        this["_content"] = content
            .replace(replace.first, replace.second)
    }

    helper.recordsService.mutate(recordAtts)
}

fun copyBpmnModule(id: String, moduleId: String) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, id)).apply {
        this["moduleId"] = moduleId
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveBpmnWithActionAndReplaceDefinition(
    resource: String,
    id: String,
    action: BpmnProcessDefActions?,
    replace: Pair<String, String>
) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, "")).apply {
        this["processDefId"] = id
        this["definition"] = ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)
            .replace(replace.first, replace.second)

        action?.let {
            this["action"] = it.name
        }
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveAndDeployBpmnFromString(bpmnData: String, id: String) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, "")).apply {
        this["processDefId"] = id
        this["definition"] = bpmnData
        this["action"] = BpmnProcessDefActions.DEPLOY.toString()
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveAndDeployDmnFromResource(resource: String, id: String) {
    saveAndDeployDmnFromString(
        ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8),
        id
    )
}

fun saveAndDeployDmnFromString(dmnData: String, id: String) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, DMN_DEF_RECORDS_SOURCE_ID, "")).apply {
        this["defId"] = id
        this["definition"] = dmnData
        this["action"] = DmnDefActions.DEPLOY.toString()
    }

    helper.recordsService.mutate(recordAtts)
}

fun saveAndDeployBpmn(elementFolder: String, id: String) {
    saveAndDeployBpmnFromResource(
        "test/bpmn/elements/$elementFolder/$id.bpmn.xml",
        id
    )
}

fun cleanDefinitions() {
    helper.procDefRevRepo.deleteAll()
    helper.procDefRepo.deleteAll()
}

fun cleanDeployments() {
    helper.camundaRepositoryService.createDeploymentQuery().list().forEach {
        helper.camundaRepositoryService.deleteDeployment(it.id, true)
    }
}

fun queryLatestProcessDefEngineRecords(forUser: String): List<EntityRef> {
    return AuthContext.runAs(forUser) {
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
        helper.recordsService.query(
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
