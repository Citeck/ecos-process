package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnEndEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TEndEvent
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records3.record.request.RequestContext
import javax.xml.namespace.QName

class BpmnEndEventConverter : EcosOmgConverter<BpmnEndEventDef, TEndEvent> {

    override fun import(element: TEndEvent, context: ImportContext): BpmnEndEventDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnEndEventDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart }
        )
    }

    override fun export(element: BpmnEndEventDef, context: ExportContext): TEndEvent {
        return TEndEvent().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, RequestContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
        }
    }
}
