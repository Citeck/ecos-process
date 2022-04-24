package ru.citeck.ecos.process.domain.bpmn.io

import javax.xml.namespace.QName

const val NS_BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL"
const val NS_ECOS = "http://www.citeck.ru/ecos/bpmn/1.0"

val BPMN_PROP_ECOS_TYPE = QName(NS_ECOS, "ecosType")
val BPMN_PROP_PROCESS_DEF_ID = QName(NS_ECOS, "processDefId")
val BPMN_PROP_NAME_ML = QName(NS_ECOS, "name_ml")
val BPMN_PROP_DOC = QName(NS_ECOS, "documentation")

val BPMN_PROP_NOTIFICATION_TEMPLATE = QName(NS_ECOS, "notificationTemplate")
val BPMN_PROP_NOTIFICATION_TYPE = QName(NS_ECOS, "notificationType")
val BPMN_PROP_NOTIFICATION_RECORD = QName(NS_ECOS, "notificationRecord")
val BPMN_PROP_NOTIFICATION_TITLE = QName(NS_ECOS, "notificationTitle")
val BPMN_PROP_NOTIFICATION_BODY = QName(NS_ECOS, "notificationBody")
val BPMN_PROP_NOTIFICATION_TO = QName(NS_ECOS, "notificationTo")
val BPMN_PROP_NOTIFICATION_CC = QName(NS_ECOS, "notificationCc")
val BPMN_PROP_NOTIFICATION_BCC = QName(NS_ECOS, "notificationBcc")
val BPMN_PROP_NOTIFICATION_LANG = QName(NS_ECOS, "notificationLang")
val BPMN_PROP_NOTIFICATION_ADDITIONAL_META = QName(NS_ECOS, "notificationAdditionalMeta")

val BPMN_PROP_ASSIGNEES = QName(NS_ECOS, "assignees")
val BPMN_PROP_FORM_REF = QName(NS_ECOS, "formRef")
val BPMN_PROP_OUTCOMES = QName(NS_ECOS, "outcomes")
val BPMN_PROP_PRIORITY = QName(NS_ECOS, "priority")

val BPMN_PROP_ECOS_BPMN_TYPE = QName(NS_ECOS, "bpmnType")
