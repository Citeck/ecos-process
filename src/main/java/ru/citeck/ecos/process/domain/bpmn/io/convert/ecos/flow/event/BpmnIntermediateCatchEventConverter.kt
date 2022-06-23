package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnIntermediateCatchEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TEventDefinition
import ru.citeck.ecos.process.domain.bpmn.model.omg.TIntermediateCatchEvent
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTimerEventDefinition
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnIntermediateCatchEventConverter : EcosOmgConverter<BpmnIntermediateCatchEventDef, TIntermediateCatchEvent> {

    override fun import(element: TIntermediateCatchEvent, context: ImportContext): BpmnIntermediateCatchEventDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnIntermediateCatchEventDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            eventDefinition = let {
                if (element.eventDefinition.size != 1) {
                    error("Not supported state. Check implementation.")
                }

                val eventDef = element.eventDefinition[0].value
                val typeToTransform = when (val type = element.eventDefinition[0].declaredType) {
                    TTimerEventDefinition::class.java -> BpmnTimerEventDef::class.java
                    else -> error("Class $type not supported")
                }

                provideOtherAttsToEventDef(eventDef, element)

                return@let context.converters.import(
                    eventDef, typeToTransform, context
                ).data
            }
        )
    }

    override fun export(element: BpmnIntermediateCatchEventDef, context: ExportContext): TIntermediateCatchEvent {
        return TIntermediateCatchEvent().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))


            when (val eventDef = element.eventDefinition) {
                is BpmnTimerEventDef -> {
                    otherAttributes.putIfNotBlank(BPMN_PROP_TIME_CONFIG, Json.mapper.toString(eventDef.value))
                }
                else -> error("Class $eventDef not supported")
            }

            val typeToTransform = when (val type = element.eventDefinition.javaClass) {
                BpmnTimerEventDef::class.java -> TTimerEventDefinition::class.java
                else -> error("Class $type not supported")
            }

            val eventDef = context.converters.export(element.eventDefinition, typeToTransform, context)
            eventDefinition.add(context.converters.convertToJaxb(eventDef))
        }
    }

    private fun provideOtherAttsToEventDef(eventDef: TEventDefinition, element: TIntermediateCatchEvent) {
        element.otherAttributes.forEach { (k, v) ->
            eventDef.otherAttributes[k] = v
        }
    }
}
