package ru.citeck.ecos.process.domain.bpmn.io

fun propMandatoryError(attr: Any, dto: Any): Nothing = throw IllegalStateException(
    "Attribute $attr is a mandatory for $dto"
)
