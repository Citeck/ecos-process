package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TErrorEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnErrorEventDefinitionConverter : EcosOmgConverter<BpmnErrorEventDef, TErrorEventDefinition> {

    override fun import(element: TErrorEventDefinition, context: ImportContext): BpmnErrorEventDef {
        val error = BpmnErrorEventDef(
            id = element.id,
            errorName = element.otherAttributes[BPMN_PROP_ERROR_NAME] ?: "",
            errorCode = element.otherAttributes[BPMN_PROP_ERROR_CODE] ?: "",
            errorMessage = element.otherAttributes[BPMN_PROP_ERROR_MESSAGE] ?: "",
            errorCodeVariable = element.otherAttributes[BPMN_PROP_ERROR_CODE_VARIABLE] ?: "",
            errorMessageVariable = element.otherAttributes[BPMN_PROP_ERROR_MESSAGE_VARIABLE] ?: "",
        )

        context.bpmnErrorEventDefs.put(error.errorName, error)

        return error
    }

    override fun export(element: BpmnErrorEventDef, context: ExportContext): TErrorEventDefinition {
        val errorId = context.bpmnErrorsByNames[element.errorName]?.id ?: error("Error not found: ${element.errorName}")

        return TErrorEventDefinition().apply {
            id = element.id
            errorRef = QName("", errorId)
        }
    }
}
