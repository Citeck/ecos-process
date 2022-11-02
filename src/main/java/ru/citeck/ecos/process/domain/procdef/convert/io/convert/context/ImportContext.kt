package ru.citeck.ecos.process.domain.procdef.convert.io.convert.context

import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters

class ImportContext(
    val converters: EcosOmgConverters,
    val bpmnSignalNames: MutableSet<String> = mutableSetOf()
)
