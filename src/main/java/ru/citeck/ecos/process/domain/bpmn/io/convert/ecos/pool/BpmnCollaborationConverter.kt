package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool

import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnMessageFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnCollaborationDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnParticipantDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCollaboration
import ru.citeck.ecos.process.domain.bpmn.model.omg.TMessageFlow
import ru.citeck.ecos.process.domain.bpmn.model.omg.TParticipant
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class BpmnCollaborationConverter : EcosOmgConverter<BpmnCollaborationDef, TCollaboration> {
    override fun import(element: TCollaboration, context: ImportContext): BpmnCollaborationDef {
        return BpmnCollaborationDef(
            id = element.id,
            participants = element.participant.map {
                context.converters.import(it, BpmnParticipantDef::class.java, context).data
            },
            messageFlows = element.messageFlow.map {
                context.converters.import(it, BpmnMessageFlowDef::class.java, context).data
            }
        )
    }

    override fun export(element: BpmnCollaborationDef, context: ExportContext): TCollaboration {
        return TCollaboration().apply {
            id = element.id

            element.participants.map {
                val tParticipant = context.converters.export(it, TParticipant::class.java, context)
                participant.add(tParticipant)
            }
            element.messageFlows.map {
                val tMessageFlow = context.converters.export(it, TMessageFlow::class.java, context)
                messageFlow.add(tMessageFlow)
            }
        }
    }
}
