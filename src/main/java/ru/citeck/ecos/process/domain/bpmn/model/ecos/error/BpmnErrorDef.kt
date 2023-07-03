package ru.citeck.ecos.process.domain.bpmn.model.ecos.error

data class BpmnErrorDef(
    val id: String,
    val name: String,
    val errorCode: String = "",
    val errorMessage: String = "",
)
