package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.process

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnSubProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TArtifact
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSubProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnSubProcessConverter : EcosOmgConverter<BpmnSubProcessDef, TSubProcess> {

    override fun import(element: TSubProcess, context: ImportContext): BpmnSubProcessDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnSubProcessDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.takeIf { it.isNotEmpty() }?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            triggeredByEvent = element.isTriggeredByEvent,
            flowElements = element.flowElement.map { it.value.toBpmnFlowElementDef(context) },
            artifacts = element.artifact.map { it.value.toBpmnArtifactDef(context) },
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            multiInstanceConfig = element.toMultiInstanceConfig()
        )
    }

    override fun export(element: BpmnSubProcessDef, context: ExportContext): TSubProcess {
        return TSubProcess().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            isTriggeredByEvent = element.triggeredByEvent

            val tFlowElements = element.flowElements.map {
                val converted = context.converters.export<TFlowElement>(it.type, it.data, context)
                context.bpmnElementsById[converted.id] = converted
                converted
            }

            val tArtifacts = element.artifacts.map {
                val converted = context.converters.export<TArtifact>(it.type, it.data, context)
                context.bpmnElementsById[converted.id] = converted
                converted
            }

            fillElementsRefsFromIdToRealObjects(tFlowElements, context)

            tFlowElements.forEach { flowElement.add(context.converters.convertToJaxb(it)) }
            tArtifacts.forEach { artifact.add(context.converters.convertToJaxb(it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))

            element.number?.let { otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, it.toString()) }
            element.multiInstanceConfig?.let {
                loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
                otherAttributes[BPMN_MULTI_INSTANCE_CONFIG] = Json.mapper.toString(it)
            }
        }
    }
}
