package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnParticipantDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TParticipant
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName

class CamundaParticipantConverter : EcosOmgConverter<BpmnParticipantDef, TParticipant> {

    override fun import(element: TParticipant, context: ImportContext): BpmnParticipantDef {
        error("Not supported")
    }

    override fun export(element: BpmnParticipantDef, context: ExportContext): TParticipant {
        return TParticipant().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())
            processRef = QName("", element.processRef)

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[BPMN_PROP_ECOS_TYPE] = element.ecosType.toString()
        }
    }
}
