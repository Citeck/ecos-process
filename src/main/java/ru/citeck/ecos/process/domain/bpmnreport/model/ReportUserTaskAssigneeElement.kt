package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportUserTaskAssigneeElement(
    var roles: ArrayList<ReportRoleElement>? = null,
    var customAssignees: ArrayList<String>? = null
)
