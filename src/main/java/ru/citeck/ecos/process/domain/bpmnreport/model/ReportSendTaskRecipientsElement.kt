package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportSendTaskRecipientsElement(
    var to: ReportSendTaskRecipientElement? = null,
    var cc: ReportSendTaskRecipientElement? = null,
    var bcc: ReportSendTaskRecipientElement? = null
)
