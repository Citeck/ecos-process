package ru.citeck.ecos.process.domain.bpmnreport.model

import ru.citeck.ecos.commons.data.MLText

data class ReportParticipantElement(
    val name: MLText? = null,
    val number: Int? = null,
    val documentation: MLText? = null
)
