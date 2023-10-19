package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportSendTaskRecipientElement(
    var roles: ArrayList<ReportRoleElement>? = null,
    var expressions: ArrayList<String>? = null
)
