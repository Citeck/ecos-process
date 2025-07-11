package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.SetStatusDelegate
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_ADD_DOCUMENT_TO_CONTEXT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_POSTPROCESSING_SCRIPT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_PREPROCESSING_SCRIPT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_AI_USER_INPUT
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_STATUS
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TASK_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaField
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaProperties
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaProperty
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnAiTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnSetStatusTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_AI
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TServiceTask
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaTaskConverter : EcosOmgConverter<BpmnTaskDef, TTask> {

    companion object {
        private const val CAMUNDA_EXTERNAL_TASK_TYPE = "external"
        private const val CITECK_AI_TASK_TOPIC = "citeck-bpmn-ai-task"
    }

    private val delegateClassFor = fun(taskDef: BpmnTaskDef): String? {
        return when (taskDef.ecosTaskDefinition) {
            is BpmnSetStatusTaskDef -> SetStatusDelegate::class.java.name
            else -> null
        }
    }

    private val ecosTaskDefFields = fun(
        element: BpmnTaskDef,
        context: ExportContext
    ): List<JAXBElement<CamundaField>> {
        val fields = mutableListOf<CamundaField>()

        when (element.ecosTaskDefinition) {
            is BpmnSetStatusTaskDef -> {
                fields.addIfNotBlank(
                    CamundaFieldCreator.string(
                        BPMN_PROP_ECOS_STATUS.localPart,
                        element.ecosTaskDefinition.status
                    )
                )
            }
        }

        return fields.map { it.jaxb(context) }
    }

    private val ecosTaskDefProperties = fun(
        element: BpmnTaskDef,
        context: ExportContext
    ): JAXBElement<CamundaProperties> {

        val properties = mutableListOf<CamundaProperty>()

        when (element.ecosTaskDefinition) {
            is BpmnAiTaskDef -> {
                properties.addIfNotBlank(
                    CamundaPropertyCreator.string(
                        BPMN_PROP_AI_USER_INPUT.localPart,
                        element.ecosTaskDefinition.userInput
                    )
                )

                properties.addIfNotBlank(
                    CamundaPropertyCreator.string(
                        BPMN_PROP_AI_ADD_DOCUMENT_TO_CONTEXT.localPart,
                        element.ecosTaskDefinition.addDocumentToContext.toString()
                    )
                )
            }
        }

        val camundaProperties = CamundaProperties().apply {
            this.properties = properties
        }

        return camundaProperties.jaxb(context)
    }

    override fun import(element: TTask, context: ImportContext): BpmnTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnTaskDef, context: ExportContext): TTask {
        val fillBaseTaskDef = fun(task: TTask) {
            with(task) {
                id = element.id
                name = MLText.getClosestValue(element.name, I18nContext.getLocale())

                element.incoming.forEach { incoming.add(QName("", it)) }
                element.outgoing.forEach { outgoing.add(QName("", it)) }

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

        if (element.ecosTaskDefinition == null) {
            return TTask().apply {
                fillBaseTaskDef(this)
            }
        }

        return TServiceTask().apply {
            fillBaseTaskDef(this)

            delegateClassFor(element)?.let {
                otherAttributes[CAMUNDA_CLASS] = it
            }

            if (element.ecosTaskDefinition is BpmnAiTaskDef) {
                otherAttributes[CAMUNDA_TYPE] = CAMUNDA_EXTERNAL_TASK_TYPE
                otherAttributes[CAMUNDA_TOPIC] = CITECK_AI_TASK_TOPIC

                otherAttributes[BPMN_PROP_ECOS_TASK_TYPE] = ECOS_TASK_AI
                otherAttributes[BPMN_PROP_AI_PREPROCESSING_SCRIPT] = element.ecosTaskDefinition.preProcessedScript
                otherAttributes[BPMN_PROP_AI_POSTPROCESSING_SCRIPT] = element.ecosTaskDefinition.postProcessedScript
                otherAttributes[BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT] =
                    element.ecosTaskDefinition.saveResultToDocumentAtt
            }

            extensionElements.any.addAll(ecosTaskDefFields(element, context))
            extensionElements.any.add(ecosTaskDefProperties(element, context))
        }
    }
}
