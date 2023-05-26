package ru.citeck.ecos.process.domain.dmn.io.convert.camunda

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_MODEL
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
        return element.dmnDef.apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            otherAttributes[DMN_PROP_MODEL] = Json.mapper.toString(element.model)
        }
    }
}
