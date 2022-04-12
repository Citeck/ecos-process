package ru.citeck.ecos.process.domain.bpmn.io

import javax.xml.namespace.QName

const val NS_BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL"
const val NS_ECOS = "http://www.citeck.ru/ecos/bpmn/1.0"

val BPMN_PROP_ECOS_TYPE = QName(NS_ECOS, "ecosType")
val BPMN_PROP_PROCESS_DEF_ID = QName(NS_ECOS, "processDefId")
val BPMN_PROP_NAME_ML = QName(NS_ECOS, "name_ml")

val BPMN_PROP_NOTIFICATION_TEMPLATE = QName(NS_ECOS, "notificationTemplate")
val BPMN_PROP_NOTIFICATION_RECORD = QName(NS_ECOS, "notificationRecord")
val BPMN_PROP_NOTIFICATION_TITLE = QName(NS_ECOS, "notificationTitle")
val BPMN_PROP_NOTIFICATION_BODY = QName(NS_ECOS, "notificationBody")

val BPMN_PROP_ECOS_BPMN_TYPE = QName(NS_ECOS, "bpmnType")
