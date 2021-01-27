package ru.citeck.ecos.process.domain.ecmmn.io.context

import ru.citeck.ecos.process.domain.cmmn.model.*

class ExportContext(
    val elementsById: MutableMap<String, TCmmnElement> = mutableMapOf()
)
