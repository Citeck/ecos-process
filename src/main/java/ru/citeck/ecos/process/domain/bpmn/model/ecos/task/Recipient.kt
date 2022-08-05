package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

data class Recipient(
    val type: RecipientType,
    val value: String
)
