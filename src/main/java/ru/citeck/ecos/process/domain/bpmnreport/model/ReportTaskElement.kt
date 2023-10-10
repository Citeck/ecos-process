package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportTaskElement(
    var outcomes: ArrayList<ReportUserTaskOutcomeElement>? = null,
    var assignees: ReportUserTaskAssigneeElement? = null,
    var recipients: ReportSendTaskRecipientsElement? = null,
    var decisionName: String? = null,
) : ReportBaseElement()
