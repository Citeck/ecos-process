package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool

import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnLaneSetDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLane
import ru.citeck.ecos.process.domain.bpmn.model.omg.TLaneSet
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaLaneSetConverter : EcosOmgConverter<BpmnLaneSetDef, TLaneSet> {

    override fun import(element: TLaneSet, context: ImportContext): BpmnLaneSetDef {
        error("Not supported")
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
