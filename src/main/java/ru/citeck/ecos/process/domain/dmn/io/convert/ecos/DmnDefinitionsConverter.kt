package ru.citeck.ecos.process.domain.dmn.io.convert.ecos

import ru.citeck.ecos.process.domain.dmn.model.ecos.DmnDefinitionDef
import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class DmnDefinitionsConverter : EcosOmgConverter<DmnDefinitionDef, TDefinitions> {
    override fun import(element: TDefinitions, context: ImportContext): DmnDefinitionDef {
        return DmnDefinitionDef(
            dmnDef = element
        )
    }

    override fun export(element: DmnDefinitionDef, context: ExportContext): TDefinitions {
        return element.dmnDef
    }
}
