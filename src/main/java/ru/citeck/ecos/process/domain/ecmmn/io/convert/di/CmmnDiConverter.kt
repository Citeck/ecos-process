package ru.citeck.ecos.process.domain.ecmmn.io.convert.di

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.*
import ru.citeck.ecos.process.domain.cmmn.service.CmmnUtils
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.model.di.CmmnDiDef
import ru.citeck.ecos.process.domain.ecmmn.model.di.diagram.CmmnDiagram
import ru.citeck.ecos.process.domain.ecmmn.model.di.diagram.CmmnDiagramElement

class CmmnDiConverter(
    private val converters: CmmnConverters
) : CmmnConverter<CMMNDI, CmmnDiDef> {

    companion object {
        const val TYPE = "DI"
    }

    override fun import(element: CMMNDI, context: ImportContext): CmmnDiDef {
        return CmmnDiDef(element.cmmnDiagram.map { importDiagram(it, context) })
    }

    private fun importDiagram(diagram: CMMNDiagram, context: ImportContext): CmmnDiagram {
        return CmmnDiagram(
            diagram.id ?: CmmnUtils.generateId("CMMNDiagram"),
            MLText(diagram.name ?: ""),
            diagram.cmmnElementRef?.localPart,
            DiagramIOUtils.convertDimension(diagram.size),
            diagram.cmmnDiagramElement?.map { importElement(it.value, context) } ?: emptyList()
        )
    }

    private fun importElement(element: DiagramElement, context: ImportContext): CmmnDiagramElement {

        val elementData = converters.import(element, context)

        return CmmnDiagramElement(
            element.id ?: CmmnUtils.generateId("DiagramElement"),
            elementData.type,
            elementData.data
        )
    }

    override fun export(element: CmmnDiDef, context: ExportContext): CMMNDI {

        val result = CMMNDI()

        element.diagrams.forEach {
            result.cmmnDiagram.add(exportDiagram(it, context))
        }
        return result
    }

    private fun exportDiagram(diagram: CmmnDiagram, context: ExportContext): CMMNDiagram {

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
