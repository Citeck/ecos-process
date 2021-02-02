package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.ExpressionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.IfPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.OnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.SentryDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import javax.xml.namespace.QName

class SentryConverter : EcosOmgConverter<SentryDef, Sentry> {

    companion object {
        private val PROP_CONDITION_TYPE = QName(CmmnXmlUtils.NS_ECOS, "conditionType")
        private val PROP_CONDITION = QName(CmmnXmlUtils.NS_ECOS, "condition")
    }

    override fun import(element: Sentry, context: ImportContext): SentryDef {

        val onPart = element.onPart?.mapNotNull { jaxbOnPart ->

            val onPartValue = jaxbOnPart.value
            val configWithType = context.converters.import(onPartValue, context)
            OnPartDef(configWithType.type, configWithType.data)

        } ?: emptyList()

        var cmmnIfPart: IfPartDef? = null
        if (element.ifPart != null) {

            val conditionType = element.ifPart.otherAttributes[PROP_CONDITION_TYPE]
            val condition = element.ifPart.otherAttributes[PROP_CONDITION]

            if (!conditionType.isNullOrBlank() && !condition.isNullOrBlank()) {
                val conditionObj = Json.mapper.read(condition, ObjectData::class.java) ?: ObjectData.create()
                cmmnIfPart = IfPartDef(element.ifPart.id, ExpressionDef(conditionType, conditionObj))
            }
        }

        return SentryDef(element.id, onPart, cmmnIfPart)
    }

    override fun export(element: SentryDef, context: ExportContext): Sentry {

        val cmmnSentry = Sentry()
        cmmnSentry.id = element.id

        element.onPart.forEach {
            val onPart = context.converters.export<TOnPart>(it.type, it.config, context)
            cmmnSentry.onPart.add(context.converters.convertToJaxb(onPart))
        }

        if (element.ifPart != null) {

            val ifPart = TIfPart()
            ifPart.id = element.ifPart.id
            ifPart.otherAttributes[PROP_CONDITION_TYPE] = element.ifPart.condition.type
            ifPart.otherAttributes[PROP_CONDITION] = Json.mapper.toString(element.ifPart.condition.config)

            val condition = TExpression()
            ifPart.condition = condition
            condition.language = "ecos"

            cmmnSentry.ifPart = ifPart
        }

        context.elementsById[cmmnSentry.id] = cmmnSentry

        return cmmnSentry
    }
}
