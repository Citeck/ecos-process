package ru.citeck.ecos.process.domain.cmmn.io.convert

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.artifact.AssociationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.di.CmmnDiConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.ActivityConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event.ExitCriterionConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.ArtifactDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.CaseDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.PlanModelDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.DiagramInterchangeDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.records2.RecordRef
import java.util.*
import javax.xml.bind.JAXBElement

class DefinitionsConverter(
    private val converters: CmmnConverters
) : CmmnConverter<Definitions, CmmnProcDef> {

    companion object {
        const val TYPE = "Definitions"
    }

    override fun import(element: Definitions, context: ImportContext): CmmnProcDef {

        val cases = element.case.map { importCase(it, context) }
        val cmmnDi = converters.import(element.cmmndi, DiagramInterchangeDef::class.java, context).data
        val artifacts = element.artifact?.map { importArtifact(it.value, context) } ?: emptyList()

        val ecosType = element.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE]
        val processDefId = element.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID]

        if (processDefId.isNullOrBlank()) {
            error("ecos:processDefId is a mandatory property for Definitions")
        }

        return CmmnProcDef(
            processDefId,
            element.id ?: CmmnXmlUtils.generateId(TYPE),
            MLText(element.name ?: ""),
            RecordRef.valueOf(ecosType),
            cases,
            artifacts,
            cmmnDi
        )
    }

    private fun importArtifact(artifact: TArtifact, context: ImportContext): ArtifactDef {
        val dataWithType = converters.import(artifact, context)
        return ArtifactDef(
            artifact.id ?: CmmnXmlUtils.generateId("Artifact"),
            dataWithType.type,
            dataWithType.data
        )
    }

    private fun importCase(case: Case, context: ImportContext): CaseDef {
        return CaseDef(
            case.id ?: CmmnXmlUtils.generateId("Case"),
            MLText(case.name ?: ""),
            importPlanModel(case.casePlanModel, context)
        )
    }

    private fun importPlanModel(plan: Stage, context: ImportContext): PlanModelDef {
        return PlanModelDef(
            plan.id,
            MLText(plan.name ?: ""),
            plan.exitCriterion?.map {
                converters.import(it, ExitCriterionDef::class.java, context).data
            } ?: emptyList(),
            plan.planItem.map {
                converters.import(it, ActivityDef::class.java, context).data
            }
        )
    }

    override fun export(element: CmmnProcDef, context: ExportContext): Definitions {

        val definitions = Definitions()
        definitions.id = element.definitionsId

        element.cases.forEach {
            definitions.case.add(exportCase(it, context))
        }

        definitions.cmmndi = converters.export(CmmnDiConverter.TYPE, element.cmmnDi, context)
        definitions.name = MLText.getClosestValue(element.name, Locale.ENGLISH)

        element.artifacts.filter {
            it.type != AssociationConverter.TYPE
        }.map {
            definitions.artifact.add(exportArtifact(it, context))
        }
        element.artifacts.filter {
            it.type == AssociationConverter.TYPE
        }.map {
            definitions.artifact.add(exportArtifact(it, context))
        }

        definitions.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE] = element.ecosType.toString()
        definitions.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID] = element.id

        definitions.targetNamespace = "http://bpmn.io/schema/cmmn"

        return definitions
    }

    private fun exportArtifact(artifact: ArtifactDef, context: ExportContext): JAXBElement<out TArtifact> {

        val tArtifact = converters.export<TArtifact>(artifact.type, artifact.data, context)
        tArtifact.id = artifact.id

        context.elementsById[artifact.id] = tArtifact
        return converters.convertToJaxb(tArtifact)
    }

    private fun exportCase(case: CaseDef, context: ExportContext): Case {

        val resultCase = Case()
        resultCase.id = case.id
        val name = MLText.getClosestValue(case.name, Locale.ENGLISH)
        if (name.isNotBlank()) {
            resultCase.name = name
        }
        resultCase.casePlanModel = exportPlanModel(case.planModel, context)

        return resultCase
    }

    private fun exportPlanModel(model: PlanModelDef, context: ExportContext): Stage {

        val casePlanModel = Stage()
        casePlanModel.id = model.id
        casePlanModel.name = MLText.getClosestValue(model.name, Locale.ENGLISH)

        model.exitCriteria.forEach {
            val criterion = converters.export<TExitCriterion>(ExitCriterionConverter.TYPE, it, context)
            casePlanModel.exitCriterion.add(criterion)
            casePlanModel.sentry.add(criterion.sentryRef as Sentry)
        }

        model.children.map { activityDef ->
            val item = converters.export<TPlanItem>(ActivityConverter.TYPE, activityDef, context)
            casePlanModel.planItem.add(item)
            casePlanModel.planItemDefinition.add(converters.convertToJaxb(item.definitionRef as TPlanItemDefinition))

            item.entryCriterion?.forEach { addSentry(casePlanModel, it.sentryRef as? Sentry) }
            item.exitCriterion?.forEach { addSentry(casePlanModel, it.sentryRef as? Sentry) }
        }

        fixSentries(casePlanModel, context)

        return casePlanModel
    }

    private fun fixSentries(stage: Stage, context: ExportContext) {
        stage.sentry.forEach { fixSentry(it, context) }
        stage.planItemDefinition.mapNotNull {
            it.value as? Stage
        }.forEach {
            fixSentries(it, context)
        }
    }

    private fun fixSentry(sentry: Sentry, context: ExportContext) {
        sentry.onPart?.mapNotNull { it.value as? TPlanItemOnPart }?.forEach {
            if (it.exitCriterionRef is String) {
                it.exitCriterionRef = context.elementsById[it.exitCriterionRef as String]
            }
            if (it.sourceRef is String) {
                it.sourceRef = context.elementsById[it.sourceRef as String]
            }
        }
    }

    private fun addSentry(stage: Stage, sentry: Sentry?) {
        sentry ?: return
        stage.sentry.add(sentry)
    }

    override fun getElementType() = TYPE
}
