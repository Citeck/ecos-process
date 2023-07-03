package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.error

import ru.citeck.ecos.process.domain.bpmn.model.ecos.error.BpmnErrorDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TError
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnErrorConverter : EcosOmgConverter<BpmnErrorDef, TError> {

    override fun import(element: TError, context: ImportContext): BpmnErrorDef {
        error("Not supported")
    }

    override fun export(element: BpmnErrorDef, context: ExportContext): TError {
        return TError().apply {
            id = element.id
            name = element.name
        }
    }
}
