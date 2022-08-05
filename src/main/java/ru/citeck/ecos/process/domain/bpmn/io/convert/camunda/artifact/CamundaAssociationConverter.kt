package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.artifact

import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnAssociationDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TAssociation
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaAssociationConverter : EcosOmgConverter<BpmnAssociationDef, TAssociation> {

    override fun import(element: TAssociation, context: ImportContext): BpmnAssociationDef {
        error("Not supported")
    }

    override fun export(element: BpmnAssociationDef, context: ExportContext): TAssociation {
        return TAssociation().apply {
            id = element.id

            sourceRef = QName("", element.sourceRef)
            targetRef = QName("", element.targetRef)
        }
    }
}
