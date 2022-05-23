package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.SYS_VAR_PREFIX
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao

@Component
class BpmnProcRecords(
    private val bpmnProcService: BpmnProcService
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

        val def = bpmnProcService.getProcessDefinitionByInstanceId(recordId) ?: return ProcRecord()

        // TODO: fill proc props to dto
        return ProcRecord(def.key)
    }

    override fun mutate(record: LocalRecordAtts): String {
        if (isAlfProcessDef(record.id)) {
            val alfRef = RecordRef.create("alfresco", "workflow", "def_${record.id}")
            val res = recordsService.mutate(alfRef, record.attributes)
            return res.id
        }

        val processVariables = mutableMapOf<String, Any?>()

        record.attributes.forEach { key, value ->
            // filter system props
            if (!key.startsWith(SYS_VAR_PREFIX)) {
                when (key) {
                    VAR_DOCUMENT -> processVariables[VAR_DOCUMENT_REF] = value.asJavaObj()
                    else -> processVariables[key] = value.asJavaObj()
                }
            }
        }

        val processInstance = bpmnProcService.startProcess(record.id, processVariables.toMap())

        return processInstance.id
    }

    data class ProcRecord(
        val key: String = ""
    ) : AttValue {

        override fun getDisplayName(): String {
            return key
        }

        override fun getType(): RecordRef {
            return RecordRef.create("emodel", "type", "bpmn-process")
        }

        override fun getAtt(name: String): Any? {
            return when (name) {
                // TODO: remove
                "foo" -> "bar"
                else -> super.getAtt(name)
            }
        }
    }

    private fun isAlfProcessDef(recordId: String): Boolean {
        return recordId.contains("$")
    }
}
