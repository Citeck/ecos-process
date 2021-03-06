package ru.citeck.ecos.process.domain.procdef.convert.io.convert.context

import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement

class ExportContext(
    val converters: EcosOmgConverters,
    val bpmnElementsById: MutableMap<String, TBaseElement> = mutableMapOf(),
    val cmmnElementsById: MutableMap<String, TCmmnElement> = mutableMapOf(),
    val cmmnPItemByDefId: MutableMap<String, TCmmnElement> = mutableMapOf()
)
