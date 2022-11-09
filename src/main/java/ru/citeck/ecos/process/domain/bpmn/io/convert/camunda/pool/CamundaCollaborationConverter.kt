package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool

import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnCollaborationDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCollaboration
import ru.citeck.ecos.process.domain.bpmn.model.omg.TParticipant
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaCollaborationConverter : EcosOmgConverter<BpmnCollaborationDef, TCollaboration> {

    override fun import(element: TCollaboration, context: ImportContext): BpmnCollaborationDef {
        error("Not supported")
    }

    override fun export(element: BpmnCollaborationDef, context: ExportContext): TCollaboration {
        return TCollaboration().apply {
            id = element.id

            element.participants.map {
                val tParticipant = context.converters.export(it, TParticipant::class.java, context)
                participant.add(tParticipant)
            }
        }
    }
}
