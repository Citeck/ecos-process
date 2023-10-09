package ru.citeck.ecos.process.domain.bpmn.api.records

import mu.KotlinLogging
import org.camunda.bpm.engine.history.HistoricProcessInstance
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.bpmn.service.ProcessInstanceQuery
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
import java.util.*

@Component
class BpmnProcessRecords(
    private val bpmnProcService: BpmnProcService,
    private val procDefService: ProcDefService
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordsQueryDao,
    RecordMutateDao {

    companion object {
        const val ID = "bpmn-proc"

        private const val BPMN_PROC_MUTATE_ACTION_FLAG = "action"

        private val log = KotlinLogging.logger {}
    }

    override fun getId(): String {
        return ID
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<EntityRef> {
        // TODO: allow query only if bpmnDefEngine is not empty? Because we need to know camunda process definition id
        // for permission check

        val procQuery = recsQuery.toProcessInstanceQuery()

        val totalCount = bpmnProcService.queryProcessInstancesCount(procQuery)
        if (totalCount == 0L) {
            return RecsQueryRes()
        }

        val instances = bpmnProcService.queryProcessInstancesMeta(procQuery)
            .map { EntityRef.create(AppName.EPROC, ID, it.id) }

        log.debug {
            "Found <$totalCount> process instances for query: \n$recsQuery. \nInstances: ${instances}"
        }

        val result = RecsQueryRes<EntityRef>()
        result.setRecords(instances)
        result.setTotalCount(totalCount)
        result.setHasMore(totalCount > recsQuery.page.maxItems + recsQuery.page.skipCount)
        return result
    }

    private fun RecordsQuery.toProcessInstanceQuery(): ProcessInstanceQuery {
        val predicate = getQuery(Predicate::class.java)
        val bpmnQuery = PredicateUtils.convertToDto(predicate, BpmnProcessRecords.BpmnProcQuery::class.java)

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

        val action = MutateAction.getFromAtts(record)

        return when (action) {
            MutateAction.START -> {
                val processInstance = startProcess(record)
                processInstance.id
            }

            MutateAction.UPDATE -> {
                updateVariables(record)
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

        return bpmnProcService.startProcess(
            record.id,
            businessKey,
            processVariables.toMap()
        )
    }

    private fun updateVariables(record: LocalRecordAtts) {
        val processInstance = bpmnProcService.getProcessInstance(record.id)
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
        bpmnProcService.setVariables(processInstance.id, processVariables)
    }

    inner class ProcRecord(
        var id: String = ""
    ) {

        private val processDefinition: ProcessDefinition? by lazy {
            if (id.isBlank()) {
                return@lazy null
            }

            bpmnProcService.getProcessDefinitionByProcessInstanceId(id)
        }

        private val processInstance: ProcessInstance? by lazy {
            if (id.isBlank()) {
                return@lazy null
            }

            bpmnProcService.getProcessInstance(id)
        }

        private val historicInstance: HistoricProcessInstance? by lazy {
            if (id.isBlank()) {
                return@lazy null
            }

            bpmnProcService.getProcessInstanceHistoricInstance(id)
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

        @AttName(".disp")
        fun getDisp(): MLText {
            return MLText(getKey())
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
            return bpmnProcService.getIncidentsByProcessInstanceId(id).map {
                EntityRef.create(AppName.EPROC, BpmnIncidentRecords.ID, it.id)
            }
        }

        @AttName("isSuspended")
        fun isSuspended(): Boolean {
            return processInstance?.isSuspended ?: false
        }
    }

    data class BpmnProcQuery(
        var document: EntityRef = EntityRef.EMPTY,
        var bpmnDefEngine: EntityRef = EntityRef.EMPTY
    )

    private enum class MutateAction {
        START,
        UPDATE;

        companion object {
            fun getFromAtts(record: LocalRecordAtts): MutateAction {
                record.attributes[BPMN_PROC_MUTATE_ACTION_FLAG].let {
                    return if (it.isNotEmpty()) {
                        valueOf(it.asText().uppercase(Locale.getDefault()))
                    } else {
                        START
                    }
                }
            }
        }
    }

    private fun isAlfProcessDef(recordId: String): Boolean {
        return recordId.contains("$")
    }
}
