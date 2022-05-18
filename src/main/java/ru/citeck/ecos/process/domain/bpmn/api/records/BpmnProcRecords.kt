package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao
import ru.citeck.ecos.records3.record.dao.mutate.RecordMutateDao

@Component
class BpmnProcRecords(
    private val runtimeService: RuntimeService,
    private val repositoryService: RepositoryService
) : AbstractRecordsDao(), RecordAttsDao,
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

       //TODO: move to proc service
        val processInstanceById = runtimeService.createProcessInstanceQuery().processInstanceId(recordId).singleResult()
            ?: return ProcRecord()
        val def = repositoryService.getProcessDefinition(processInstanceById.processDefinitionId)

        //TODO: fill proc props to dto
        return ProcRecord(def.key)
    }

    override fun mutate(record: LocalRecordAtts): String {
        if (isAlfProcessDef(record.id)) {
            val alfRef = RecordRef.create("alfresco", "workflow", "def_${record.id}")
            val res = recordsService.mutate(alfRef, record.attributes)
            return res.id
        }

        val variables = mutableMapOf<String, Any?>()

        record.attributes.forEach { key, value ->
            //filter system props
            if (!key.startsWith("_")) {
                variables[key] = value.asJavaObj()
            }
        }

        //TODO: move to proc service
        val processInstance = runtimeService.startProcessInstanceByKey("Process_${record.id}", variables)

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
                //TODO: remove
                "foo" -> "bar"
                else -> super.getAtt(name)
            }

        }
    }

    private fun isAlfProcessDef(recordId: String): Boolean {
        return recordId.contains("$")
    }
}
