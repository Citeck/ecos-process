package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ASSIGNEE_ELEMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.toCamundaCode
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.recipientsToJsonWithoutType
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.BpmnUserTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.omg.TMultiInstanceLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaUserTaskConverter : EcosOmgConverter<BpmnUserTaskDef, TUserTask> {

    val usersExpression = fun(roles: List<String>): String {
        return "\${roles.getUserNames($BPMN_DOCUMENT_REF, '${roles.joinToString(BPMN_CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    val groupsExpression = fun(roles: List<String>): String {
        return "\${roles.getGroupNames($BPMN_DOCUMENT_REF, '${roles.joinToString(BPMN_CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    val authorityNamesExpression = fun(roles: List<String>): String {
        return "\${roles.getAuthorityNames($BPMN_DOCUMENT_REF, '${roles.joinToString(BPMN_CAMUNDA_COLLECTION_SEPARATOR)}')}"
    }

    override fun import(element: TUserTask, context: ImportContext): BpmnUserTaskDef {
        error("Not supported")
    }

    override fun export(element: BpmnUserTaskDef, context: ExportContext): TUserTask {
        return TUserTask().apply {
            id = element.id

            name = MLText.getClosestValue(element.name, I18nContext.getLocale())
            otherAttributes[BPMN_PROP_NAME_ML] = element.name.toString()

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            if (element.manualRecipientsMode) {
                // Use assignee as storage for manual recipients with expression support.
                // A comma is added if there is only one recipient, so that the camunda perceive the recipient as string
                // see ManualRecipientsModeUserTaskAssignListener
                otherAttributes[CAMUNDA_ASSIGNEE] = let {
                    var recipientsStr = element.manualRecipients.joinToString(BPMN_CAMUNDA_COLLECTION_SEPARATOR)
                    val isSingleRecipient = element.manualRecipients.size == 1 &&
                        !recipientsStr.contains(BPMN_CAMUNDA_COLLECTION_SEPARATOR)

                    if (isSingleRecipient) {
                        recipientsStr += BPMN_CAMUNDA_COLLECTION_SEPARATOR
                    }
                    return@let recipientsStr
                }

                element.multiInstanceConfig?.let {
                    loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
                }
            } else {
                if (element.multiInstanceAutoMode && element.multiInstanceConfig != null) {
                    loopCharacteristics = context.converters.convertToJaxb(
                        element.multiInstanceConfig.toAutoModeLoopCharacteristics(element)
                    )
                } else {
                    // Use assignee as storage for Ecos Roles recipients.
                    // see RecipientsFromRolesUserTaskAssignListener
                    otherAttributes[CAMUNDA_ASSIGNEE] = let {
                        usersExpression(element.assignees.map { it.value }) + "," +
                            groupsExpression(element.assignees.map { it.value })
                    }
                }
            }

            val priority = if (element.priorityExpression.isNullOrBlank()) {
                element.priority.toCamundaCode().toString()
            } else {
                element.priorityExpression
            }
            otherAttributes[CAMUNDA_PRIORITY] = priority

            otherAttributes[CAMUNDA_FORM_KEY] = element.formRef.toString()

            otherAttributes.putIfNotBlank(CAMUNDA_DUE_DATE, element.dueDate)
            otherAttributes.putIfNotBlank(CAMUNDA_FOLLOW_UP_DATE, element.followUpDate)

            otherAttributes[BPMN_PROP_MANUAL_RECIPIENTS_MODE] = element.manualRecipientsMode.toString()
            otherAttributes[BPMN_PROP_MULTI_INSTANCE_AUTO_MODE] = element.multiInstanceAutoMode.toString()
            otherAttributes[BPMN_PROP_LA_ENABLED] = element.laEnabled.toString()
            otherAttributes[BPMN_PROP_LA_NOTIFICATION_TYPE] = element.laNotificationType.toString()
            otherAttributes[BPMN_PROP_LA_NOTIFICATION_TEMPLATE] = element.laNotificationTemplate.toString()
            otherAttributes.putIfNotBlank(BPMN_PROP_ASSIGNEES, recipientsToJsonWithoutType(element.assignees))

            otherAttributes.putIfNotBlank(BPMN_PROP_OUTCOMES, Json.mapper.toString(element.outcomes))
        }
    }

    private fun MultiInstanceConfig.toAutoModeLoopCharacteristics(element: BpmnUserTaskDef): TLoopCharacteristics {
        return TMultiInstanceLoopCharacteristics().apply {
            isIsSequential = sequential

            otherAttributes.putIfNotBlank(
                CAMUNDA_COLLECTION,
                authorityNamesExpression(
                    element.assignees.map { it.value }
                )
            )
            otherAttributes.putIfNotBlank(CAMUNDA_ELEMENT_VARIABLE, BPMN_ASSIGNEE_ELEMENT)
        }
    }
}
