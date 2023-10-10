package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DOC
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnParticipantDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TParticipant
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnParticipantConverter : EcosOmgConverter<BpmnParticipantDef, TParticipant> {

    override fun import(element: TParticipant, context: ImportContext): BpmnParticipantDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnParticipantDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            processRef = element.processRef.localPart,
            ecosType = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_ECOS_TYPE])
        )
    }

    override fun export(element: BpmnParticipantDef, context: ExportContext): TParticipant {
        return TParticipant().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())
            processRef = QName("", element.processRef)

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[BPMN_PROP_ECOS_TYPE] = element.ecosType.toString()

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
        }
    }
}
