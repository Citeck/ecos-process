package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_SCRIPT_ENGINE_LANGUAGE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.getCamundaJobRetryTimeCycleFieldConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.scriptPayloadToTScript
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.script.BpmnScriptTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TScriptTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaScriptTaskConverter : EcosOmgConverter<BpmnScriptTaskDef, TScriptTask> {

    override fun import(element: TScriptTask, context: ImportContext): BpmnScriptTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnScriptTaskDef, context: ExportContext): TScriptTask {
        return TScriptTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            script = element.scriptPayloadToTScript()
            scriptFormat = DEFAULT_SCRIPT_ENGINE_LANGUAGE

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
