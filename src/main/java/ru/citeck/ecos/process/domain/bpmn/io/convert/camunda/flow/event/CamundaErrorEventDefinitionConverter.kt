package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ERROR_CODE_VARIABLE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ERROR_MESSAGE_VARIABLE
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TErrorEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaErrorEventDefinitionConverter : EcosOmgConverter<BpmnErrorEventDef, TErrorEventDefinition> {

    override fun import(element: TErrorEventDefinition, context: ImportContext): BpmnErrorEventDef {
        error("Not supported")
    }

    override fun export(element: BpmnErrorEventDef, context: ExportContext): TErrorEventDefinition {
        val errorId = context.bpmnErrorsByNames[element.errorName]?.id
            ?: error("Error with name ${element.errorName} not found")

        return TErrorEventDefinition().apply {
            id = element.id
            errorRef = QName("", errorId)

            otherAttributes.putIfNotBlank(CAMUNDA_ERROR_CODE_VARIABLE, element.errorCodeVariable)
            otherAttributes.putIfNotBlank(CAMUNDA_ERROR_MESSAGE_VARIABLE, element.errorMessageVariable)
        }
    }
}
