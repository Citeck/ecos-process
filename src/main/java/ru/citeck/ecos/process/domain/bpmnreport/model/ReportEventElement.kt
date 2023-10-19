package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportEventElement(
    override var type: String = "",
    override var name: MLText? = null,
    override var documentation: MLText? = null,

    var eventType: MLText? = null,
    var value: String? = null,
) : ReportBaseElement()
