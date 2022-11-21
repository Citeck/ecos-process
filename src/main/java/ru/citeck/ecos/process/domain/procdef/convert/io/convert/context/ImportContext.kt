package ru.citeck.ecos.process.domain.procdef.convert.io.convert.context

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters

class ImportContext(
    val converters: EcosOmgConverters,
    val bpmnSignalEventDefs: MutableList<BpmnSignalEventDef> = mutableListOf()
)
