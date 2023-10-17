package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toMultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.service.BpmnServiceTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.service.ServiceTaskType
import ru.citeck.ecos.process.domain.bpmn.model.omg.TServiceTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class BpmnServiceTaskConverter : EcosOmgConverter<BpmnServiceTaskDef, TServiceTask> {

    override fun import(element: TServiceTask, context: ImportContext): BpmnServiceTaskDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnServiceTaskDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.takeIf { it.isNotEmpty() }?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            type = ServiceTaskType.valueOf(
                element.otherAttributes[BPMN_PROP_SERVICE_TASK_TYPE] ?: error("Service task type is not defined")
            ),
            externalTaskTopic = element.otherAttributes[BPMN_PROP_EXTERNAL_TASK_TOPIC] ?: "",
            expression = element.otherAttributes[BPMN_PROP_EXPRESSION] ?: "",
            resultVariable = element.otherAttributes[BPMN_PROP_RESULT_VARIABLE] ?: "",
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            multiInstanceConfig = element.toMultiInstanceConfig()
        )
    }

    override fun export(element: BpmnServiceTaskDef, context: ExportContext): TServiceTask {
        return TServiceTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_SERVICE_TASK_TYPE, element.type.name)
            otherAttributes.putIfNotBlank(BPMN_PROP_EXTERNAL_TASK_TOPIC, element.externalTaskTopic)
            otherAttributes.putIfNotBlank(BPMN_PROP_EXPRESSION, element.expression)
            otherAttributes.putIfNotBlank(BPMN_PROP_RESULT_VARIABLE, element.resultVariable)
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
