package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.getCamundaJobRetryTimeCycleFieldConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule.BpmnBusinessRuleTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBusinessRuleTask
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaBusinessRuleTaskConverter : EcosOmgConverter<BpmnBusinessRuleTaskDef, TBusinessRuleTask> {
    override fun import(element: TBusinessRuleTask, context: ImportContext): BpmnBusinessRuleTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnBusinessRuleTaskDef, context: ExportContext): TBusinessRuleTask {
        return TBusinessRuleTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[CAMUNDA_DECISION_REF] = element.decisionRefKey
            otherAttributes[CAMUNDA_DECISION_REF_BINDING] = element.binding.value
            otherAttributes.putIfNotBlank(CAMUNDA_DECISION_REF_VERSION, element.version?.toString())
            otherAttributes.putIfNotBlank(CAMUNDA_DECISION_REF_VERSION_TAG, element.versionTag)
            otherAttributes.putIfNotBlank(CAMUNDA_RESULT_VARIABLE, element.resultVariable)
            otherAttributes.putIfNotBlank(CAMUNDA_MAP_DECISION_RESULT, element.mapDecisionResult?.value)

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
