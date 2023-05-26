package ru.citeck.ecos.process.domain.dmn.io.convert.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.propMandatoryError
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_DEF_ID
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_MODEL
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.dmn.model.ecos.DmnDefinitionDef
import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class DmnDefinitionsConverter : EcosOmgConverter<DmnDefinitionDef, TDefinitions> {
    override fun import(element: TDefinitions, context: ImportContext): DmnDefinitionDef {

        val defId = element.otherAttributes[DMN_PROP_DEF_ID]
        if (defId.isNullOrBlank()) {
            propMandatoryError(DMN_PROP_DEF_ID, BpmnDefinitionDef::class)
        }

        val name = element.otherAttributes[DMN_PROP_NAME_ML] ?: element.name

        @Suppress("UNCHECKED_CAST")
        return DmnDefinitionDef(
            id = defId,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            model = Json.mapper.convert(
                element.otherAttributes[DMN_PROP_MODEL],
                Map::class.java
            ) as Map<String, String>? ?: emptyMap(),
            dmnDef = element
        )
    }

    override fun export(element: DmnDefinitionDef, context: ExportContext): TDefinitions {
        return element.dmnDef.apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            otherAttributes[DMN_PROP_MODEL] = Json.mapper.toString(element.model)
        }
    }
}
