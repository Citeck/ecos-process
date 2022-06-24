package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_AFTER
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_BEFORE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_JOB_PRIORITY
import ru.citeck.ecos.process.domain.bpmn.io.convert.fillCamundaEventDefPayloadFromBpmnEventDef
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnStartEventDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TStartEvent
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaStartEventConverter : EcosOmgConverter<BpmnStartEventDef, TStartEvent> {

    override fun import(element: TStartEvent, context: ImportContext): BpmnStartEventDef {
        error("Not supported")
    }

    override fun export(element: BpmnStartEventDef, context: ExportContext): TStartEvent {
        return TStartEvent().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes[CAMUNDA_ASYNC_BEFORE] = element.asyncConfig.asyncBefore.toString()
            otherAttributes[CAMUNDA_ASYNC_AFTER] = element.asyncConfig.asyncAfter.toString()
            otherAttributes.putIfNotBlank(CAMUNDA_JOB_PRIORITY, element.jobConfig.jobPriority.toString())

            fillCamundaEventDefPayloadFromBpmnEventDef(element.eventDefinition, element.jobConfig, context)
        }
    }
}
