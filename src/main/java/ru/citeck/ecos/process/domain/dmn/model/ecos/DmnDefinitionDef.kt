package ru.citeck.ecos.process.domain.dmn.model.ecos

import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions

/**
 * At this moment, we don't want to have Ecos DMN format, so we just store TDefinitions.
 */
data class DmnDefinitionDef(
    val dmnDef: TDefinitions
)
