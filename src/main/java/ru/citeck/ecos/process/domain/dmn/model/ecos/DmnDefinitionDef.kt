package ru.citeck.ecos.process.domain.dmn.model.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions

/**
 * At this moment, we don't want to have Ecos DMN format, so we just store TDefinitions.
 */
data class DmnDefinitionDef(
    val id: String,
    val name: MLText,
    val model: Map<String, String> = emptyMap(),

    val dmnDef: TDefinitions
)
