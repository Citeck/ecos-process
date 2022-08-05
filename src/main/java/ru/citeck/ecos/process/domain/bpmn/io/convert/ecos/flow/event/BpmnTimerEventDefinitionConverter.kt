package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_TIME_CONFIG
import ru.citeck.ecos.process.domain.bpmn.io.convert.createTExpressionWithContent
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.time.TimeType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.time.TimeValue
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTimerEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnTimerEventDefinitionConverter : EcosOmgConverter<BpmnTimerEventDef, TTimerEventDefinition> {

    override fun import(element: TTimerEventDefinition, context: ImportContext): BpmnTimerEventDef {
        return BpmnTimerEventDef(
            id = element.id,
            value = Json.mapper.read(element.otherAttributes[BPMN_PROP_TIME_CONFIG], TimeValue::class.java)
                ?: error("Time value in mandatory for BpmnTimerEventDef"),
        )
    }

    override fun export(element: BpmnTimerEventDef, context: ExportContext): TTimerEventDefinition {
        return TTimerEventDefinition().apply {
            id = element.id

            when (element.value.type) {
                TimeType.DATE -> timeDate = createTExpressionWithContent(element.value.value)
                TimeType.DURATION -> timeDuration = createTExpressionWithContent(element.value.value)
                TimeType.CYCLE -> timeCycle = createTExpressionWithContent(element.value.value)
            }
        }
    }
}
