package ru.citeck.ecos.process.domain.cmmn.io.context

import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverters
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement

class ExportContext(
    val converters: EcosOmgConverters,
    val elementsById: MutableMap<String, TCmmnElement> = mutableMapOf(),
    val planItemByDefId: MutableMap<String, TCmmnElement> = mutableMapOf()
)
