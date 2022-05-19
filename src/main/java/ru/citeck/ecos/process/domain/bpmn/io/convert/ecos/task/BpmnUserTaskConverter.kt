package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsFromJson
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsToJsonWithoutType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskPriority
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.request.RequestContext
import javax.xml.namespace.QName

class BpmnUserTaskConverter : EcosOmgConverter<BpmnUserTaskDef, TUserTask> {

    override fun import(element: TUserTask, context: ImportContext): BpmnUserTaskDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnUserTaskDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            outcomes = Json.mapper.readList(element.otherAttributes[BPMN_PROP_OUTCOMES], TaskOutcome::class.java),
            assignees = recipientsFromJson(
                RecipientType.ROLE, element.otherAttributes[BPMN_PROP_ASSIGNEES] ?: ""
            ),
            formRef = RecordRef.valueOf(element.otherAttributes[BPMN_PROP_FORM_REF]),
            priority = TaskPriority.valueOf(element.otherAttributes[BPMN_PROP_PRIORITY]!!),
        )
    }

    override fun export(element: BpmnUserTaskDef, context: ExportContext): TUserTask {
        return TUserTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_OUTCOMES, Json.mapper.toString(element.outcomes))
            otherAttributes.putIfNotBlank(BPMN_PROP_ASSIGNEES, recipientsToJsonWithoutType(element.assignees))
            otherAttributes.putIfNotBlank(BPMN_PROP_FORM_REF, element.formRef.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_PRIORITY, element.priority.toString())
        }
    }
}
