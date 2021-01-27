package ru.citeck.ecos.process.domain.ecmmn.io.convert.plan.event

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.model.*
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverter
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.CmmnExpressionDef
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnOnPart
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.event.CmmnSentryDef

class SentryConverter(private val converters: CmmnConverters) : CmmnConverter<Sentry, CmmnSentryDef> {

    companion object {
        const val TYPE = "Sentry"
    }

    override fun import(element: Sentry, context: ImportContext): CmmnSentryDef {

        val onPart = element.onPart?.mapNotNull { jaxbOnPart ->

            val tOnPart = jaxbOnPart.value as? TPlanItemOnPart ?: return@mapNotNull null
            val onPartSource = tOnPart.sourceRef as? TPlanItem ?: return@mapNotNull null

            val exitSentryRef = (tOnPart.exitCriterionRef as? TCriterion)?.let { (it.sentryRef as? Sentry)?.id }

            CmmnOnPart(
                tOnPart.id,
                onPartSource.id,
                tOnPart.standardEvent.value(),
                exitSentryRef
            )
        } ?: emptyList()

        var cmmnIfPart: CmmnExpressionDef? = null
        if (element.ifPart != null && element.ifPart.condition != null) {
            val condition = element.ifPart.condition
            if (condition.content != null && condition.content.isNotEmpty()) {
                val conditionConfig = condition.content[0] as? String
                if (conditionConfig != null) {
                    cmmnIfPart = CmmnExpressionDef(
                        condition.language,
                        Json.mapper.read(conditionConfig, ObjectData::class.java)!!
                    )
                }
            }
        }

        return CmmnSentryDef(element.id, onPart, cmmnIfPart)
    }

    override fun export(element: CmmnSentryDef, context: ExportContext): Sentry {

        val cmmnSentry = Sentry()
        cmmnSentry.id = element.id

        element.onPart.forEach {

            val cmmnOnPart = TPlanItemOnPart()
            if (it.exitEventRef != null) {
                cmmnOnPart.exitCriterionRef = it.exitEventRef
            }
            cmmnOnPart.id = it.id
            cmmnOnPart.standardEvent = PlanItemTransition.fromValue(it.eventType)
            cmmnOnPart.sourceRef = it.sourceRef

            cmmnSentry.onPart.add(converters.convertToJaxb(cmmnOnPart))
        }

        if (element.ifPart != null) {

            val ifPart = TIfPart()
            val condition = TExpression()
            ifPart.condition = condition

            condition.language = element.ifPart.type
            condition.content.add(Json.mapper.toString(element.ifPart.config))

            cmmnSentry.ifPart = ifPart
        }

        context.elementsById[cmmnSentry.id] = cmmnSentry

        return cmmnSentry
    }

    override fun getElementType() = TYPE
}
