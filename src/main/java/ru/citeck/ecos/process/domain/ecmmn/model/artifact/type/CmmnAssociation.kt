package ru.citeck.ecos.process.domain.ecmmn.model.artifact.type

class CmmnAssociation(
    val sourceRef: String,
    val targetRef: String,
    val direction: CmmnAssocDirection = CmmnAssocDirection.NONE
)
