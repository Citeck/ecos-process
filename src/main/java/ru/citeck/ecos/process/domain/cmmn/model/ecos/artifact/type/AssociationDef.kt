package ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type

class AssociationDef(
    val sourceRef: String,
    val targetRef: String,
    val direction: AssocDirectionEnum = AssocDirectionEnum.NONE
)
