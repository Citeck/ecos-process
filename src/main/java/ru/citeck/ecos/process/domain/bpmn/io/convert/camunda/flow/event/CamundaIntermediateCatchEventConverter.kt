package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_AFTER
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_BEFORE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_JOB_PRIORITY
import ru.citeck.ecos.process.domain.bpmn.io.convert.getCamundaJobRetryTimeCycleFieldConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnIntermediateCatchEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TIntermediateCatchEvent
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTimerEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaIntermediateCatchEventConverter :
    EcosOmgConverter<BpmnIntermediateCatchEventDef, TIntermediateCatchEvent> {

    override fun import(element: TIntermediateCatchEvent, context: ImportContext): BpmnIntermediateCatchEventDef {
        error("Not supported")
    }

    override fun export(element: BpmnIntermediateCatchEventDef, context: ExportContext): TIntermediateCatchEvent {
        return TIntermediateCatchEvent().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[CAMUNDA_ASYNC_BEFORE] = element.asyncConfig.asyncBefore.toString()
            otherAttributes[CAMUNDA_ASYNC_AFTER] = element.asyncConfig.asyncAfter.toString()
            otherAttributes.putIfNotBlank(CAMUNDA_JOB_PRIORITY, element.jobConfig.jobPriority.toString())

            val typeToTransform = when (val type = element.eventDefinition.javaClass) {
                BpmnTimerEventDef::class.java -> TTimerEventDefinition::class.java
                else -> error("Class $type not supported")
            }

            val eventDef = context.converters.export(element.eventDefinition, typeToTransform, context)
            eventDefinition.add(context.converters.convertToJaxb(eventDef))

            extensionElements = TExtensionElements().apply {
                any.addAll(getCamundaJobRetryTimeCycleFieldConfig(element.jobConfig.jobRetryTimeCycle, context))
            }
        }
    }
}
