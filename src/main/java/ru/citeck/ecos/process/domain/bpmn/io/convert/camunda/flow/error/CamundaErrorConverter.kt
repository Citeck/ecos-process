package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.error

import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ERROR_MESSAGE
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.error.BpmnErrorDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TError
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaErrorConverter : EcosOmgConverter<BpmnErrorDef, TError> {

    override fun import(element: TError, context: ImportContext): BpmnErrorDef {
        error("Not supported")
    }

    override fun export(element: BpmnErrorDef, context: ExportContext): TError {
        return TError().apply {
            id = element.id
            name = element.name

            if (element.errorCode.isNotEmpty()) {
                errorCode = element.errorCode
            }

            otherAttributes.putIfNotBlank(CAMUNDA_ERROR_MESSAGE, element.errorMessage)
        }
    }
}
