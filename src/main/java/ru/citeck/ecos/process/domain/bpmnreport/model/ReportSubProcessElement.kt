package ru.citeck.ecos.process.domain.bpmnreport.model

data class ReportSubProcessElement(
    var elements: List<String>? = null,
    var subProcessName: String? = null
) : ReportBaseElement()
