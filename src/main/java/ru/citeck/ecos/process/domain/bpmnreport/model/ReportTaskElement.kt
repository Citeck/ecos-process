package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportTaskElement(
    override var type: String = "",
    override var name: MLText? = null,
    override var documentation: MLText? = null,

    var outcomes: ArrayList<ReportUserTaskOutcomeElement>? = null,
    var assignees: ReportUserTaskAssigneeElement? = null,
    var recipients: ReportSendTaskRecipientsElement? = null,
    var decisionName: String? = null,
    var service: ReportServiceTaskDefElement? = null
) : ReportBaseElement()
