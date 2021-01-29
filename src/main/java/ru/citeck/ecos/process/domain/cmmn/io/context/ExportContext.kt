package ru.citeck.ecos.process.domain.cmmn.io.context

import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement

class ExportContext(
    val elementsById: MutableMap<String, TCmmnElement> = mutableMapOf()
)
