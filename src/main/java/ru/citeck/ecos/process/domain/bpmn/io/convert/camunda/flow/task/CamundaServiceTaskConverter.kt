package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.getCamundaJobRetryTimeCycleFieldConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.service.BpmnServiceTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.service.ServiceTaskType
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TServiceTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import java.util.*
import javax.xml.namespace.QName

class CamundaServiceTaskConverter : EcosOmgConverter<BpmnServiceTaskDef, TServiceTask> {

    override fun import(element: TServiceTask, context: ImportContext): BpmnServiceTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnServiceTaskDef, context: ExportContext): TServiceTask {
        return TServiceTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            // set camunda:type only for external tasks
            if (element.type == ServiceTaskType.EXTERNAL) {
                otherAttributes[CAMUNDA_TYPE] = element.type.name.lowercase(Locale.getDefault())
            }

            otherAttributes.putIfNotBlank(CAMUNDA_TOPIC, element.externalTaskTopic)
            otherAttributes.putIfNotBlank(CAMUNDA_EXPRESSION, element.expression)
            otherAttributes.putIfNotBlank(CAMUNDA_RESULT_VARIABLE, element.resultVariable)

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
