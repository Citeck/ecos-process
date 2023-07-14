package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.common.toProcessKey
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_CALLED_ELEMENT_BINDING
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_CALLED_ELEMENT_VERSION
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_CALLED_ELEMENT_VERSION_TAG
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_JOB_PRIORITY
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.callactivity.BpmnCallActivityDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCallActivity
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

private const val CAMUNDA_PROPAGATION_PROCESS_BUSINESS_KEY_EXPRESSION = "#{execution.processBusinessKey}"

class CamundaCallActivityTaskConverter : EcosOmgConverter<BpmnCallActivityDef, TCallActivity> {
    override fun import(element: TCallActivity, context: ImportContext): BpmnCallActivityDef {
        error("Not supported")
    }

    override fun export(element: BpmnCallActivityDef, context: ExportContext): TCallActivity {
        return TCallActivity().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            calledElement = if (element.calledElement.isNullOrBlank()) {
                QName("", element.processRef.toProcessKey())
            } else {
                QName("", element.calledElement)
            }
            otherAttributes[CAMUNDA_CALLED_ELEMENT_BINDING] = element.binding.value
            otherAttributes.putIfNotBlank(CAMUNDA_CALLED_ELEMENT_VERSION, element.version?.toString())
            otherAttributes.putIfNotBlank(CAMUNDA_CALLED_ELEMENT_VERSION_TAG, element.versionTag)

            otherAttributes.putIfNotBlank(CAMUNDA_JOB_PRIORITY, element.jobConfig.jobPriority.toString())

            extensionElements = TExtensionElements().apply {
                any.addAll(getCamundaJobRetryTimeCycleFieldConfig(element.jobConfig.jobRetryTimeCycle, context))

                any.add(
                    getCamundaBusinessKeyPropagationInInElement(
                        CAMUNDA_PROPAGATION_PROCESS_BUSINESS_KEY_EXPRESSION,
                        context
                    )
                )
                any.addAll(getDefaultInVariablesPropagationToCallActivity(context))

                any.addAll(element.inVariablePropagation.toCamundaInElements(context))
                any.addAll(element.outVariablePropagation.toCamundaOutElements(context))
            }

            element.multiInstanceConfig?.let {
                loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
            }
        }
    }
}
