package ru.citeck.ecos.process.domain.bpmn.model.ecos

class EcosBpmnElementDefinitionException(id: String, msg: String) : RuntimeException("$id: $msg")

class EcosBpmnDefinitionException(msg: String) : RuntimeException(msg)
