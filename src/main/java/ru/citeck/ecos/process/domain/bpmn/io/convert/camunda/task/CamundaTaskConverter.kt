package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.task.SetStatusDelegate
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_STATUS
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaField
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnSetStatusTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TServiceTask
import ru.citeck.ecos.process.domain.bpmn.model.omg.TTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class CamundaTaskConverter : EcosOmgConverter<BpmnTaskDef, TTask> {

    private val delegateClass = fun(taskDef: BpmnTaskDef): String? {
        if (taskDef.ecosTaskDefinition == null) {
            return null
        }

        return when (taskDef.ecosTaskDefinition) {
            is BpmnSetStatusTaskDef -> SetStatusDelegate::class.java.name
            else -> error("Unsupported task definition: ${taskDef.ecosTaskDefinition}")
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
                    CamundaFieldCreator.string(BPMN_PROP_ECOS_STATUS.localPart, element.ecosTaskDefinition.status)
                )
            }
        }

        return fields.map { it.jaxb(context) }
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

            delegateClass(element)?.let {
                otherAttributes[CAMUNDA_CLASS] = it
            }

            extensionElements = TExtensionElements()
            extensionElements.any.addAll(ecosTaskDefFields(element, context))
        }
    }
}
