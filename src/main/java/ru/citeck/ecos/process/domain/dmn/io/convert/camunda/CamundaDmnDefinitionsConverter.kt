package ru.citeck.ecos.process.domain.dmn.io.convert.camunda

import ru.citeck.ecos.process.domain.dmn.model.ecos.DmnDefinitionDef
import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaDmnDefinitionsConverter : EcosOmgConverter<DmnDefinitionDef, TDefinitions> {
    override fun import(element: TDefinitions, context: ImportContext): DmnDefinitionDef {
        error("Not supported")
    }

    override fun export(element: DmnDefinitionDef, context: ExportContext): TDefinitions {
        return element.dmnDef
    }
}
