package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef

@Component
class BpmnProcRecords(
    private val bpmnProcService: BpmnProcService,
    private val procDefService: ProcDefService
) : AbstractRecordsDao(),
    RecordAttsDao,
    RecordMutateDao {

    companion object {
        const val ID = "bpmn-proc"
    }

    override fun getId(): String {
        return ID
    }

    override fun getRecordAtts(recordId: String): Any? {
        if (isAlfProcessDef(recordId)) {
            var ref = RecordRef.valueOf(recordId)
            if (ref.appName.isBlank()) {
                ref = ref.withAppName("alfresco")
            }

            if (ref.sourceId.isBlank()) {
                ref = ref.withSourceId("workflow")
            }

            return ref
        }

        val def = bpmnProcService.getProcessDefinitionByProcessInstanceId(recordId) ?: return ProcRecord()
        val defRev = procDefService.getProcessDefRevByDeploymentId(def.deploymentId) ?: return ProcRecord()

        return ProcRecord(def.key, EntityRef.create(AppName.EPROC, BpmnProcDefVersionRecords.ID, defRev.id.toString()))
    }

    override fun mutate(record: LocalRecordAtts): String {
        if (isAlfProcessDef(record.id)) {
            val alfRef = RecordRef.create("alfresco", "workflow", "def_${record.id}")
            val res = recordsService.mutate(alfRef, record.attributes)
            return res.id
        }

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

        val processInstance = bpmnProcService.startProcess(
            record.id,
            businessKey,
            processVariables.toMap()
        )

        return processInstance.id
    }

    class ProcRecord(
        val key: String = "",
        val definitionRef: EntityRef = EntityRef.EMPTY
    ) {

        @AttName(".disp")
        fun getDisp(): MLText {
            return MLText(key)
        }
    }

    private fun isAlfProcessDef(recordId: String): Boolean {
        return recordId.contains("$")
    }
}
