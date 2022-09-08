package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.process

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_AFTER
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_BEFORE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_EXCLUSIVE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_JOB_PRIORITY
import ru.citeck.ecos.process.domain.bpmn.io.convert.getCamundaJobRetryTimeCycleFieldConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnSubProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TArtifact
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSubProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaSubProcessConverter : EcosOmgConverter<BpmnSubProcessDef, TSubProcess> {

    override fun import(element: TSubProcess, context: ImportContext): BpmnSubProcessDef {
        error("Not supported")
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

            otherAttributes[CAMUNDA_ASYNC_BEFORE] = element.asyncConfig.asyncBefore.toString()
            otherAttributes[CAMUNDA_ASYNC_AFTER] = element.asyncConfig.asyncAfter.toString()
            otherAttributes[CAMUNDA_EXCLUSIVE] = element.asyncConfig.exclusive.toString()

            otherAttributes.putIfNotBlank(CAMUNDA_JOB_PRIORITY, element.jobConfig.jobPriority.toString())

            extensionElements = TExtensionElements().apply {
                any.addAll(getCamundaJobRetryTimeCycleFieldConfig(element.jobConfig.jobRetryTimeCycle, context))
            }

            element.multiInstanceConfig?.let {
                loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
            }
        }
    }
}
