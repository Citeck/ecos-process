package ru.citeck.ecos.process.domain.cmmn.io.convert.di

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.DiagramInterchangeDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram.DiagramDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.diagram.DiagramElementDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.CMMNDI
import ru.citeck.ecos.process.domain.cmmn.model.omg.CMMNDiagram
import ru.citeck.ecos.process.domain.cmmn.model.omg.DiagramElement

class CmmnDiConverter(
    private val converters: CmmnConverters
) : CmmnConverter<CMMNDI, DiagramInterchangeDef> {

    companion object {
        const val TYPE = "DI"
    }

    override fun import(element: CMMNDI, context: ImportContext): DiagramInterchangeDef {
        return DiagramInterchangeDef(element.cmmnDiagram.map { importDiagram(it, context) })
    }

    private fun importDiagram(diagram: CMMNDiagram, context: ImportContext): DiagramDef {
        return DiagramDef(
            diagram.id ?: CmmnXmlUtils.generateId("CMMNDiagram"),
            MLText(diagram.name ?: ""),
            diagram.cmmnElementRef?.localPart,
            DiagramIOUtils.convertDimension(diagram.size),
            diagram.cmmnDiagramElement?.map { importElement(it.value, context) } ?: emptyList()
        )
    }

    private fun importElement(element: DiagramElement, context: ImportContext): DiagramElementDef {

        val elementData = converters.import(element, context)

        return DiagramElementDef(
            element.id ?: CmmnXmlUtils.generateId("DiagramElement"),
            elementData.type,
            elementData.data
        )
    }

    override fun export(element: DiagramInterchangeDef, context: ExportContext): CMMNDI {

        val result = CMMNDI()

        element.diagrams.forEach {
            result.cmmnDiagram.add(exportDiagram(it, context))
        }
        return result
    }

    private fun exportDiagram(diagram: DiagramDef, context: ExportContext): CMMNDiagram {

        val resultDiagram = CMMNDiagram()
        resultDiagram.id = diagram.id
        resultDiagram.size = DiagramIOUtils.convertDimension(diagram.size)

        diagram.elements.forEach {
            val elem = converters.export<DiagramElement>(it.type, it.data, context)
            resultDiagram.cmmnDiagramElement.add(converters.convertToJaxb(elem))
        }
        return resultDiagram
    }

    override fun getElementType() = TYPE
}
