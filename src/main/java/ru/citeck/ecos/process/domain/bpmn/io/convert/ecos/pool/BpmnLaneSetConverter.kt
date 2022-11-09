package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool

import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnLaneDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnLaneSetDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLane
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLaneSet
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnLaneSetConverter : EcosOmgConverter<BpmnLaneSetDef, TLaneSet> {

    override fun import(element: TLaneSet, context: ImportContext): BpmnLaneSetDef {
        return BpmnLaneSetDef(
            id = element.id,
            lanes = element.lane.map { context.converters.import(it, BpmnLaneDef::class.java, context).data }
        )
    }

    override fun export(element: BpmnLaneSetDef, context: ExportContext): TLaneSet {
        return TLaneSet().apply {
            id = element.id
            element.lanes.map {
                val tLane = context.converters.export(it, TLane::class.java, context)
                lane.add(tLane)
            }
        }
    }
}
