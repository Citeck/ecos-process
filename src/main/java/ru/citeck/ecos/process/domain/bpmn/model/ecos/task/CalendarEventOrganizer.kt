package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

import ru.citeck.ecos.commons.utils.StringUtils

data class CalendarEventOrganizer(
    val role: String,
    val expression: String
) {

    fun isEmpty() = StringUtils.isBlank(role) && StringUtils.isBlank(expression)
}
