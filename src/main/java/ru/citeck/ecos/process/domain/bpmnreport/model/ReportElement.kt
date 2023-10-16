package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportElement(
    val id: String,
    val number: Int?,
    val process: ReportProcessElement,
    var lane: ReportLaneElement? = null,
    var annotations: ArrayList<ReportAnnotationElement>? = null,
    var incoming: List<ReportSequenceElement>? = null,
    var eventElement: ReportEventElement? = null,
    var statusElement: ReportStatusElement? = null,
    var gatewayElement: ReportBaseElement? = null,
    var taskElement: ReportTaskElement? = null,
    var subProcessElement: ReportSubProcessElement? = null
)
