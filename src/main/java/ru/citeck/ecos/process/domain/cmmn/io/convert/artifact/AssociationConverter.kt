package ru.citeck.ecos.process.domain.cmmn.io.convert.artifact

import ru.citeck.ecos.process.domain.cmmn.model.omg.TAssociation
import ru.citeck.ecos.process.domain.cmmn.model.omg.TAssociationDirection
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.AssocDirectionEnum
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.AssociationDef

class AssociationConverter: CmmnConverter<TAssociation, AssociationDef> {

    companion object {
        const val TYPE = "Association"
    }

    override fun import(element: TAssociation, context: ImportContext): AssociationDef {

        val source = element.sourceRef
        if (source !is TCmmnElement) {
            error("Incorrect source: $source")
        }
        val target = element.targetRef
        if (target !is TCmmnElement) {
            error("Incorrect target: $target")
        }

        return AssociationDef(
            source.id,
            target.id,
            element.associationDirection?.let {
                AssocDirectionEnum.valueOf(it.toString())
            } ?: AssocDirectionEnum.NONE
        )
    }

    override fun export(element: AssociationDef, context: ExportContext): TAssociation {

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
