package ru.citeck.ecos.process.domain.bpmn.io

import javax.xml.namespace.QName

const val NS_BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL"
const val NS_ECOS_BPMN = "http://www.citeck.ru/ecos/bpmn/1.0"
const val NS_XSI = "http://www.w3.org/2001/XMLSchema-instance"

val XSI_TYPE = QName(NS_XSI, "type")
val SCRIPT_LANGUAGE_ATTRIBUTE = QName("", "language")
const val BPMN_T_FORMAT_EXPRESSION = "bpmn:tFormalExpression"

val BPMN_PROP_ECOS_TYPE = QName(NS_ECOS_BPMN, "ecosType")
val BPMN_PROP_ENABLED = QName(NS_ECOS_BPMN, "enabled")
val BPMN_PROP_AUTO_START_ENABLED = QName(NS_ECOS_BPMN, "autoStartEnabled")
val BPMN_PROP_SECTION_REF = QName(NS_ECOS_BPMN, "sectionRef")
val BPMN_PROP_PROCESS_DEF_ID = QName(NS_ECOS_BPMN, "processDefId")
val BPMN_PROP_NAME_ML = QName(NS_ECOS_BPMN, "name_ml")
val BPMN_PROP_DOC = QName(NS_ECOS_BPMN, "documentation")

val BPMN_PROP_NOTIFICATION_TEMPLATE = QName(NS_ECOS_BPMN, "notificationTemplate")
val BPMN_PROP_NOTIFICATION_TYPE = QName(NS_ECOS_BPMN, "notificationType")
val BPMN_PROP_NOTIFICATION_RECORD = QName(NS_ECOS_BPMN, "notificationRecord")
val BPMN_PROP_NOTIFICATION_TITLE = QName(NS_ECOS_BPMN, "notificationTitle")
val BPMN_PROP_NOTIFICATION_BODY = QName(NS_ECOS_BPMN, "notificationBody")
val BPMN_PROP_NOTIFICATION_FROM = QName(NS_ECOS_BPMN, "notificationFrom")
val BPMN_PROP_NOTIFICATION_TO = QName(NS_ECOS_BPMN, "notificationTo")
val BPMN_PROP_NOTIFICATION_TO_EXPRESSION = QName(NS_ECOS_BPMN, "notificationToExpression")
val BPMN_PROP_NOTIFICATION_CC = QName(NS_ECOS_BPMN, "notificationCc")
val BPMN_PROP_NOTIFICATION_CC_EXPRESSION = QName(NS_ECOS_BPMN, "notificationCcExpression")
val BPMN_PROP_NOTIFICATION_BCC = QName(NS_ECOS_BPMN, "notificationBcc")
val BPMN_PROP_NOTIFICATION_BCC_EXPRESSION = QName(NS_ECOS_BPMN, "notificationBccExpression")
val BPMN_PROP_NOTIFICATION_LANG = QName(NS_ECOS_BPMN, "notificationLang")
val BPMN_PROP_NOTIFICATION_ADDITIONAL_META = QName(NS_ECOS_BPMN, "notificationAdditionalMeta")

val BPMN_PROP_ASSIGNEES = QName(NS_ECOS_BPMN, "assignees")
val BPMN_PROP_FORM_REF = QName(NS_ECOS_BPMN, "formRef")
val BPMN_PROP_OUTCOMES = QName(NS_ECOS_BPMN, "outcomes")
val BPMN_PROP_PRIORITY = QName(NS_ECOS_BPMN, "priority")
val BPMN_PROP_DUE_DATE = QName(NS_ECOS_BPMN, "dueDate")
val BPMN_PROP_FOLLOW_UP_DATE = QName(NS_ECOS_BPMN, "followUpDate")

val BPMN_PROP_MANUAL_SIGNAL_NAME = QName(NS_ECOS_BPMN, "manualSignalName")
val BPMN_PROP_EVENT_MANUAL_MODE = QName(NS_ECOS_BPMN, "eventManualMode")
val BPMN_PROP_EVENT_TYPE = QName(NS_ECOS_BPMN, "eventType")
val BPMN_PROP_EVENT_FILTER_BY_RECORD_TYPE = QName(NS_ECOS_BPMN, "eventFilterByRecordType")
val BPMN_PROP_EVENT_FILTER_BY_ECOS_TYPE = QName(NS_ECOS_BPMN, "eventFilterByEcosType")
val BPMN_PROP_EVENT_FILTER_BY_RECORD_VARIABLE = QName(NS_ECOS_BPMN, "eventFilterByRecordVariable")
val BPMN_PROP_EVENT_FILTER_BY_PREDICATE = QName(NS_ECOS_BPMN, "eventFilterByPredicate")
val BPMN_PROP_EVENT_MODEL = QName(NS_ECOS_BPMN, "eventModel")

val BPMN_PROP_MULTI_INSTANCE_AUTO_MODE = QName(NS_ECOS_BPMN, "multiInstanceAutoMode")
val BPMN_PROP_MANUAL_RECIPIENTS_MODE = QName(NS_ECOS_BPMN, "manualRecipientsMode")
val BPMN_PROP_MANUAL_RECIPIENTS = QName(NS_ECOS_BPMN, "manualRecipients")

val BPMN_PROP_CONDITION_CONFIG = QName(NS_ECOS_BPMN, "conditionConfig")
val BPMN_PROP_CONDITION_TYPE = QName(NS_ECOS_BPMN, "conditionType")

val BPMN_PROP_ECOS_BPMN_TYPE = QName(NS_ECOS_BPMN, "bpmnType")

val BPMN_PROP_RESULT_VARIABLE = QName(NS_ECOS_BPMN, "resultVariable")
val BPMN_PROP_ASYNC_CONFIG = QName(NS_ECOS_BPMN, "asyncConfig")
val BPMN_PROP_JOB_CONFIG = QName(NS_ECOS_BPMN, "jobConfig")
val BPMN_PROP_TIME_CONFIG = QName(NS_ECOS_BPMN, "timeConfig")
val BPMN_PROP_SCRIPT = QName(NS_ECOS_BPMN, "script")

val BPMN_MULTI_INSTANCE_CONFIG = QName(NS_ECOS_BPMN, "multiInstanceConfig")

val BPMN_PROP_ECOS_TASK_TYPE = QName(NS_ECOS_BPMN, "taskType")
val BPMN_PROP_ECOS_STATUS = QName(NS_ECOS_BPMN, "status")
