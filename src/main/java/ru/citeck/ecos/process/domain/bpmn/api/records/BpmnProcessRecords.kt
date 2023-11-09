package ru.citeck.ecos.process.domain.bpmn.api.records

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.camunda.bpm.engine.history.HistoricProcessInstance
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.rest.ProcessInstanceRestService
import org.camunda.bpm.engine.rest.dto.runtime.modification.ProcessInstanceModificationDto
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.service.*
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.time.Instant

@Component
class BpmnProcessRecords(
    private val bpmnProcessService: BpmnProcessService,
    private val procDefService: ProcDefService,
    private val camundaProcessInstanceRestService: ProcessInstanceRestService
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDao {

    companion object {
        const val ID = "bpmn-proc"

        private const val BPMN_PROC_MUTATE_ACTION_FLAG = "action"
        private const val ATT_ON_DELETE_SKIP_CUSTOM_LISTENER = "skipCustomListener"
        private const val ATT_ON_DELETE_SKIP_IO_MAPPING = "skipIoMapping"
        private const val ATT_DATA = "data"

        // We can't use our Json.mapper for convert ProcessInstanceModificationDto
        private val standardMapper = ObjectMapper()

        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        val procQuery = recsQuery.toProcessInstanceQuery()

        if (!BpmnPermission.PROC_INSTANCE_READ.isAllowForBpmnDefEngine(procQuery.bpmnDefEngine)) {
            return RecsQueryRes()
        }

        val totalCount = bpmnProcessService.queryProcessInstancesCount(procQuery)
        if (totalCount == 0L) {
            return RecsQueryRes()
        }

        val instances = bpmnProcessService.queryProcessInstancesMeta(procQuery)
            .map { EntityRef.create(AppName.EPROC, ID, it.id) }

        log.debug {
            "Found <$totalCount> process instances for query: \n$recsQuery. \nInstances: $instances"
        }

        val result = RecsQueryRes<EntityRef>()
        result.setRecords(instances)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)
        return result
    }

    private fun RecordsQuery.toProcessInstanceQuery(): ProcessInstanceQuery {
        val predicate = getQuery(Predicate::class.java)
        val bpmnQuery = PredicateUtils.convertToDto(predicate, BpmnProcQuery::class.java)

        check(bpmnQuery.bpmnDefEngine.isNotEmpty() && bpmnQuery.bpmnDefEngine.getLocalId().isNotBlank()) {
            "Bpmn definition engine is mandatory for query process instances"
        }

        return ProcessInstanceQuery(
            businessKey = bpmnQuery.document.toString(),
            bpmnDefEngine = bpmnQuery.bpmnDefEngine,
            page = page,
            sortBy = if (sortBy.isEmpty()) {
                SortBy.create().build()
            } else {
                sortBy[0]
            }
        )
    }

    override fun getRecordAtts(recordId: String): Any? {
        if (isAlfProcessDef(recordId)) {
            var ref = RecordRef.valueOf(recordId)
            if (ref.appName.isBlank()) {
                ref = ref.withAppName(AppName.ALFRESCO)
            }

            if (ref.sourceId.isBlank()) {
                ref = ref.withSourceId("workflow")
            }

            return ref
        }

        if (!BpmnPermission.PROC_INSTANCE_READ.isAllowForProcessInstanceId(recordId)) {
            return null
        }

        return ProcRecord(
            recordId
        )
    }

    override fun mutate(record: LocalRecordAtts): String {
        if (isAlfProcessDef(record.id)) {
            val alfRef = RecordRef.create(AppName.ALFRESCO, "workflow", "def_${record.id}")
            val res = recordsService.mutate(alfRef, record.attributes)
            return res.id
        }

        val action = record.toActionEnumOrDefault(MutateAction::class.java, MutateAction.START)

        return when (action) {
            MutateAction.START -> {
                check(BpmnPermission.PROC_INSTANCE_RUN.isAllowForProcessKey(record.id)) {
                    "User ${AuthContext.getCurrentUser()} has no permission to start process instance: ${record.id}"
                }

                val processInstance = startProcess(record)
                processInstance.id
            }

            MutateAction.UPDATE -> {
                check(BpmnPermission.PROC_INSTANCE_EDIT.isAllowForProcessInstanceId(record.id)) {
                    "User ${AuthContext.getCurrentUser()} has no permission to update process instance: ${record.id}"
                }

                updateVariables(record)
                record.id
            }

            MutateAction.DELETE -> {
                check(BpmnPermission.PROC_INSTANCE_EDIT.isAllowForProcessInstanceId(record.id)) {
                    "User ${AuthContext.getCurrentUser()} has no permission to delete process instance: ${record.id}"
                }

                delete(record)
                record.id
            }

            MutateAction.SUSPEND -> {
                check(BpmnPermission.PROC_INSTANCE_EDIT.isAllowForProcessInstanceId(record.id)) {
                    "User ${AuthContext.getCurrentUser()} has no permission to suspend process instance: ${record.id}"
                }

                bpmnProcessService.suspendProcess(record.id)
                record.id
            }

            MutateAction.ACTIVATE -> {
                check(BpmnPermission.PROC_INSTANCE_EDIT.isAllowForProcessInstanceId(record.id)) {
                    "User ${AuthContext.getCurrentUser()} has no permission to activate process instance: ${record.id}"
                }

                bpmnProcessService.activateProcess(record.id)
                record.id
            }

            MutateAction.MODIFY -> {
                check(BpmnPermission.PROC_INSTANCE_MIGRATE.isAllowForProcessInstanceId(record.id)) {
                    "User ${AuthContext.getCurrentUser()} has no permission to move token of process instance: " +
                        record.id
                }

                modify(record)
                record.id
            }
        }
    }

    private fun startProcess(record: LocalRecordAtts): ProcessInstance {
        val processVariables = mutableMapOf<String, Any?>()
        var businessKey: String? = null

        record.attributes.forEach { key, value ->
            // filter system props
            if (!key.startsWith(SYS_VAR_PREFIX)) {
                when (key) {
                    BPMN_DOCUMENT -> {
                        val documentRef = value.asText()
                        processVariables[BPMN_DOCUMENT_REF] = documentRef
                        val typeId = AuthContext.runAsSystem {
                            recordsService.getAtt(
                                documentRef.toEntityRef(),
                                RecordConstants.ATT_TYPE + ScalarType.LOCAL_ID_SCHEMA
                            ).asText()
                        }
                        if (typeId.isNotBlank()) {
                            processVariables[BPMN_DOCUMENT_TYPE] = typeId
                        }
                        businessKey = value.asJavaObj().toString()
                    }

                    else -> processVariables[key] = value.asJavaObj()
                }
            }
        }

        log.debug { "Starting process ${record.id}, businessKey: $businessKey with variables: \n$processVariables" }

        return bpmnProcessService.startProcess(
            record.id,
            businessKey,
            processVariables.toMap()
        )
    }

    private fun updateVariables(record: LocalRecordAtts) {
        val processInstance = bpmnProcessService.getProcessInstance(record.id)
            ?: throw IllegalArgumentException("Process instance not found: ${record.id}")

        val processVariables = mutableMapOf<String, Any?>()

        record.attributes.forEach { key, value ->
            if (key == BPMN_PROC_MUTATE_ACTION_FLAG) {
                return@forEach
            }

            processVariables[key] = value.asJavaObj()
        }

        log.debug {
            "Updating process instance ${processInstance.id} with variables: \n$processVariables"
        }
        bpmnProcessService.setVariables(processInstance.id, processVariables)
    }

    private fun delete(record: LocalRecordAtts) {
        val skipCustomListener = record.getAtt(ATT_ON_DELETE_SKIP_CUSTOM_LISTENER).asBoolean()
        val skipIoMapping = record.getAtt(ATT_ON_DELETE_SKIP_IO_MAPPING).asBoolean()

        bpmnProcessService.deleteProcessInstance(
            record.id,
            skipCustomListener,
            skipIoMapping
        )
    }

    private fun modify(record: LocalRecordAtts) {
        val id = record.id
        val modifyInstruction = record.getAtt(ATT_DATA).toString()
        if (modifyInstruction.isBlank()) {
            throw IllegalArgumentException("Modify instruction is empty")
        }

        val dto: ProcessInstanceModificationDto = standardMapper.readValue(
            modifyInstruction,
            ProcessInstanceModificationDto::class.java
        )

        camundaProcessInstanceRestService.getProcessInstance(id).modifyProcessInstance(dto)
    }

    inner class ProcRecord(
        var id: String = ""
    ) {

        private val processDefinition: ProcessDefinition? by lazy {
            if (id.isBlank()) {
                return@lazy null
            }

            bpmnProcessService.getProcessDefinitionByProcessInstanceId(id)
        }

        private val processInstance: ProcessInstance? by lazy {
            if (id.isBlank()) {
                return@lazy null
            }

            bpmnProcessService.getProcessInstance(id)
        }

        private val historicInstance: HistoricProcessInstance? by lazy {
            if (id.isBlank()) {
                return@lazy null
            }

            bpmnProcessService.getProcessInstanceHistoricInstance(id)
        }

        @AttName(RecordConstants.ATT_TYPE)
        fun getType(): EntityRef {
            return EntityRef.create(AppName.EMODEL, "type", "bpmn-process")
        }

        @AttName("ecosDefRev")
        fun getDefinitionVersionRef(): EntityRef {
            val deploymentId = getDeploymentId()
            if (deploymentId.isBlank()) {
                return EntityRef.EMPTY
            }

            val defRev = procDefService.getProcessDefRevByDeploymentId(deploymentId) ?: return EntityRef.EMPTY
            return EntityRef.create(AppName.EPROC, BpmnProcessDefVersionRecords.ID, defRev.id.toString())
        }

        @AttName("deploymentId")
        fun getDeploymentId(): String {
            return processDefinition?.deploymentId ?: ""
        }

        @AttName("key")
        fun getKey(): String {
            return processDefinition?.key ?: ""
        }

        @AttName("bpmnDefEngine")
        fun getBpmnDefEngine(): EntityRef {
            return processDefinition?.id?.takeIf { it.isNotBlank() }
                ?.let { BpmnProcessDefEngineRecords.createRef(it) }
                ?: EntityRef.EMPTY
        }

        @AttName(".disp")
        fun getDisp(): MLText {
            return MLText(
                I18nContext.ENGLISH to "Process instance: $id",
                I18nContext.RUSSIAN to "Экземпляр процесса: $id"
            )
        }

        @AttName("businessKey")
        fun getBusinessKey(): String {
            return processInstance?.businessKey ?: ""
        }

        @AttName("documentRef")
        fun getDocumentRef(): EntityRef {
            return EntityRef.valueOf(getBusinessKey())
        }

        @AttName("startTime")
        fun getStartTime(): Instant? {
            return historicInstance?.startTime?.toInstant()
        }

        @AttName("incidents")
        fun getIncidents(): List<EntityRef> {
            return bpmnProcessService.getIncidentsByProcessInstanceId(id).map {
                EntityRef.create(AppName.EPROC, BpmnIncidentRecords.ID, it.id)
            }
        }

        @AttName("isSuspended")
        fun isSuspended(): Boolean {
            return processInstance?.isSuspended ?: false
        }

        @AttName("activityStatistics")
        fun getActivityStatistics(): List<ActivityStatistics> {
            return bpmnProcessService.getProcessInstanceActivityStatistics(id)
        }
    }

    data class BpmnProcQuery(
        var document: EntityRef = EntityRef.EMPTY,
        var bpmnDefEngine: EntityRef = EntityRef.EMPTY
    )

    enum class MutateAction {
        START,
        UPDATE,
        DELETE,
        SUSPEND,
        ACTIVATE,
        MODIFY
    }

    private fun isAlfProcessDef(recordId: String): Boolean {
        return recordId.contains("$")
    }
}
