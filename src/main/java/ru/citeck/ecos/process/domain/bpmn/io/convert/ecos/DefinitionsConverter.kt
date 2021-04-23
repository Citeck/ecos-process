package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos

import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter

class DefinitionsConverter : EcosOmgConverter<BpmnProcessDef, TDefinitions> {

    override fun import(element: TDefinitions, context: ImportContext): BpmnProcessDef {

        val process = element.rootElement[0].value as TProcess
        println(process.id)

        error("Unsupported")
    }

    override fun export(element: BpmnProcessDef, context: ExportContext): TDefinitions {
        error("Unsupported")
    }
}
