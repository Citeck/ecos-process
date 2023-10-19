package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportSubProcessElement(
    override var type: String = "",
    override var name: MLText? = null,
    override var documentation: MLText? = null,

    var elements: List<String>? = null,
    var subProcessName: String? = null
) : ReportBaseElement()
