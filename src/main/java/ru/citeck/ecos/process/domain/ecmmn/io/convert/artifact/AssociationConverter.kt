package ru.citeck.ecos.process.domain.ecmmn.io.convert.artifact

import ru.citeck.ecos.process.domain.cmmn.model.TAssociation
import ru.citeck.ecos.process.domain.cmmn.model.TAssociationDirection
import ru.citeck.ecos.process.domain.cmmn.model.TCmmnElement
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.artifact.type.CmmnAssocDirection
import ru.citeck.ecos.process.domain.ecmmn.model.artifact.type.CmmnAssociation

class AssociationConverter: CmmnConverter<TAssociation, CmmnAssociation> {

    companion object {
        const val TYPE = "Association"
    }

    override fun import(element: TAssociation, context: ImportContext): CmmnAssociation {

        val source = element.sourceRef
        if (source !is TCmmnElement) {
            error("Incorrect source: $source")
        }
        val target = element.targetRef
        if (target !is TCmmnElement) {
            error("Incorrect target: $target")
        }

        return CmmnAssociation(
            source.id,
            target.id,
            element.associationDirection?.let {
                CmmnAssocDirection.valueOf(it.toString())
            } ?: CmmnAssocDirection.NONE
        )
    }

    override fun export(element: CmmnAssociation, context: ExportContext): TAssociation {

        val tAssoc = TAssociation()
        tAssoc.sourceRef = context.elementsById[element.sourceRef]
        tAssoc.targetRef = context.elementsById[element.targetRef]
        tAssoc.associationDirection = TAssociationDirection.valueOf(element.direction.toString())
        if (tAssoc.associationDirection == TAssociationDirection.NONE) {
            tAssoc.associationDirection = null
        }

        return tAssoc
    }

    override fun getElementType() = TYPE
}
