package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.process.domain.bpmn.io.convert.createTExpressionWithContent
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.time.TimeType
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTimerEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaTimerEventDefinitionConverter : EcosOmgConverter<BpmnTimerEventDef, TTimerEventDefinition> {

    override fun import(element: TTimerEventDefinition, context: ImportContext): BpmnTimerEventDef {
        error("Not supported")
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
