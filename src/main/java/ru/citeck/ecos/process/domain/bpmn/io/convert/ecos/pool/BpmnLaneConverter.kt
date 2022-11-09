package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnLaneDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLane
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnLaneConverter : EcosOmgConverter<BpmnLaneDef, TLane> {
    override fun import(element: TLane, context: ImportContext): BpmnLaneDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnLaneDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            flowRefs = element.flowNodeRef.map { flowElement ->
                val tBase = flowElement.value as? TBaseElement
                    ?: error("Flow ref is not TBaseElement. Late id: ${element.id}")
                tBase.id
            }
        )
    }

    override fun export(element: BpmnLaneDef, context: ExportContext): TLane {
        return TLane().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.flowRefs.map {
                val realObject = context.bpmnElementsById[it]
                    ?: error("Flow ref not found, id: ${element.id}")
                flowNodeRef.add(context.converters.convertToJaxbFlowNodeRef(realObject))
            }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
        }
    }
}
