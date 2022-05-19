package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.task

import org.camunda.bpm.model.bpmn.impl.BpmnModelConstants.*
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.toCamundaCode
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records3.record.request.RequestContext
import javax.xml.namespace.QName

class CamundaUserTaskConverter : EcosOmgConverter<BpmnUserTaskDef, TUserTask> {

    private val camundaCandidateUsers = QName(CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CANDIDATE_USERS)
    private val camundaCandidateGroups = QName(CAMUNDA_NS, CAMUNDA_ATTRIBUTE_CANDIDATE_GROUPS)
    private val camundaPriority = QName(CAMUNDA_NS, CAMUNDA_ATTRIBUTE_PRIORITY)

    val usersExpression = fun(roles: List<String>): String {
        return "\${roles.getUserNames(document, '${roles.joinToString(CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    val groupsExpression = fun(roles: List<String>): String {
        return "\${roles.getGroupNames(document, '${roles.joinToString(CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    override fun import(element: TUserTask, context: ImportContext): BpmnUserTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnUserTaskDef, context: ExportContext): TUserTask {
        return TUserTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[camundaCandidateUsers] = usersExpression(element.assignees.map { it.value })
            otherAttributes[camundaCandidateGroups] = groupsExpression(element.assignees.map { it.value })
            otherAttributes[camundaPriority] = element.priority.toCamundaCode().toString()

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes.putIfNotBlank(BPMN_PROP_OUTCOMES, Json.mapper.toString(element.outcomes))
            otherAttributes.putIfNotBlank(BPMN_PROP_FORM_REF, element.formRef.toString())
        }
    }
}
