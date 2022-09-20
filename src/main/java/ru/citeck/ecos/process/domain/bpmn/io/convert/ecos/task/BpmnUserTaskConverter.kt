package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records2.RecordRef
import javax.xml.namespace.QName

class BpmnUserTaskConverter : EcosOmgConverter<BpmnUserTaskDef, TUserTask> {

    override fun import(element: TUserTask, context: ImportContext): BpmnUserTaskDef {
        val nameMl = let {
            val name = Json.mapper.convert(element.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java)
                ?: MLText.EMPTY
            return@let if (name == MLText.EMPTY) {
                MLText(element.name)
            } else {
                name
            }
        }

        return BpmnUserTaskDef(
            id = element.id,
            name = nameMl,
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            outcomes = Json.mapper.readList(element.otherAttributes[BPMN_PROP_OUTCOMES], TaskOutcome::class.java),
            manualRecipientsMode = element.otherAttributes[BPMN_PROP_MANUAL_RECIPIENTS_MODE]?.toBoolean() ?: false,
            manualRecipients = Json.mapper.readList(
                element.otherAttributes[BPMN_PROP_MANUAL_RECIPIENTS],
                String::class.java
            ),
            assignees = recipientsFromJson(
                RecipientType.ROLE,
                element.otherAttributes[BPMN_PROP_ASSIGNEES] ?: ""
            ),
            formRef = RecordRef.valueOf(element.otherAttributes[BPMN_PROP_FORM_REF]),
            priority = TaskPriority.valueOf(element.otherAttributes[BPMN_PROP_PRIORITY]!!),
            multiInstanceConfig = element.toMultiInstanceConfig()
        )
    }

    override fun export(element: BpmnUserTaskDef, context: ExportContext): TUserTask {
        return TUserTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_OUTCOMES, Json.mapper.toString(element.outcomes))
            otherAttributes.putIfNotBlank(BPMN_PROP_ASSIGNEES, recipientsToJsonWithoutType(element.assignees))
            otherAttributes.putIfNotBlank(BPMN_PROP_FORM_REF, element.formRef.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_PRIORITY, element.priority.toString())

            otherAttributes.putIfNotBlank(BPMN_PROP_MANUAL_RECIPIENTS_MODE, element.manualRecipientsMode.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_MANUAL_RECIPIENTS, Json.mapper.toString(element.manualRecipients))

            element.multiInstanceConfig?.let {
                loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
                otherAttributes[BPMN_MULTI_INSTANCE_CONFIG] = Json.mapper.toString(it)
            }
        }
    }
}
