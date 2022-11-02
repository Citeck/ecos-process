package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.signal

import ru.citeck.ecos.process.domain.bpmn.model.ecos.signal.BpmnSignalDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSignal
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnSignalConverter : EcosOmgConverter<BpmnSignalDef, TSignal> {

    override fun import(element: TSignal, context: ImportContext): BpmnSignalDef {
        error("Not supported")
    }

    override fun export(element: BpmnSignalDef, context: ExportContext): TSignal {
        return TSignal().apply {
            id = element.id
            name = element.name
        }
    }
}
