package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.task

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.*
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.toCamundaCode
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_OUTCOMES
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_CANDIDATE_GROUPS
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_CANDIDATE_USERS
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_FORM_KEY
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_PRIORITY
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaUserTaskConverter : EcosOmgConverter<BpmnUserTaskDef, TUserTask> {

    val usersExpression = fun(roles: List<String>): String {
        return "\${roles.getUserNames($VAR_DOCUMENT_REF, '${roles.joinToString(CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    val groupsExpression = fun(roles: List<String>): String {
        return "\${roles.getGroupNames($VAR_DOCUMENT_REF, '${roles.joinToString(CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    override fun import(element: TUserTask, context: ImportContext): BpmnUserTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnUserTaskDef, context: ExportContext): TUserTask {
        return TUserTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[CAMUNDA_CANDIDATE_USERS] = usersExpression(element.assignees.map { it.value })
            otherAttributes[CAMUNDA_CANDIDATE_GROUPS] = groupsExpression(element.assignees.map { it.value })
            otherAttributes[CAMUNDA_PRIORITY] = element.priority.toCamundaCode().toString()
            otherAttributes[CAMUNDA_FORM_KEY] = element.formRef.toString()

            otherAttributes.putIfNotBlank(BPMN_PROP_OUTCOMES, Json.mapper.toString(element.outcomes))
        }
    }
}
