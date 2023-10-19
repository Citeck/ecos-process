package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportProcessElement(
    val id: String,
    var participant: ReportParticipantElement? = null
)
