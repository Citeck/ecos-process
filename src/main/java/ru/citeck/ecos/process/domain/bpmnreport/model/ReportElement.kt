package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.ObjectData

data class ReportElement(
    val id: String,
    val type: String, // Временный параметр
    val data: ObjectData, // Временный параметр
    val number: String?,
    var lane: ReportLaneElement? = null,
    var annotations: ArrayList<ReportAnnotationElement>? = null,
    var incoming: List<ReportSequenceElement>? = null,
    var eventElement: ReportEventElement? = null,
    var statusElement: ReportStatusElement? = null,
    var gatewayElement: ReportBaseElement? = null,
    var taskElement: ReportTaskElement? = null
)
