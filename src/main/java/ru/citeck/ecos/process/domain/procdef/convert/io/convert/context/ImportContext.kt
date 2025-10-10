package ru.citeck.ecos.process.domain.procdef.convert.io.convert.context

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnConditionalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters

class ImportContext(
    var definitionEnabled: Boolean = false,
    val converters: EcosOmgConverters,
    val validateRequired: Boolean = true,
    val bpmnSignalEventDefs: MutableList<BpmnSignalEventDef> = mutableListOf(),
    val bpmnErrorEventDefs: MutableMap<String, BpmnErrorEventDef> = mutableMapOf(),
    val conditionalEventDefs: MutableList<BpmnConditionalEventDef> = mutableListOf()
)
