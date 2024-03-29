package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact

import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.AssocDirectionEnum
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.AssociationDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.TAssociation
import ru.citeck.ecos.process.domain.cmmn.model.omg.TAssociationDirection
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class AssociationConverter : EcosOmgConverter<AssociationDef, TAssociation> {

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
        tAssoc.sourceRef = context.cmmnElementsById[element.sourceRef]
        tAssoc.targetRef = context.cmmnElementsById[element.targetRef]
        tAssoc.associationDirection = TAssociationDirection.valueOf(element.direction.toString())
        if (tAssoc.associationDirection == TAssociationDirection.NONE) {
            tAssoc.associationDirection = null
        }

        return tAssoc
    }
}
