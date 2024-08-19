package ru.citeck.ecos.process.domain.cmmn.io.convert.ecos

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.cmmn.io.CmmnFormat
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.ArtifactDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.artifact.type.AssociationDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.CaseDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.PlanModelDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.activity.ActivityDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.ExitCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.di.DiagramInterchangeDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.ConvertUtils
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class DefinitionsConverter : EcosOmgConverter<CmmnProcessDef, Definitions> {

    override fun import(element: Definitions, context: ImportContext): CmmnProcessDef {

        val cases = element.case.map { importCase(it, context) }
        val cmmnDi = context.converters.import(element.cmmndi, DiagramInterchangeDef::class.java, context).data
        val artifacts = element.artifact?.map { importArtifact(it.value, context) } ?: emptyList()

        val ecosType = element.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE]
        val processDefId = element.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID]

        if (processDefId.isNullOrBlank()) {
            error("ecos:processDefId is a mandatory property for Definitions")
        }

        val name = element.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] ?: element.name

        val otherData = element.otherAttributes.filter {
            it.key.namespaceURI == CmmnXmlUtils.NS_ECOS &&
                it.key.localPart.startsWith("other_")
        }.map {
            it.key.localPart.substring("other_".length) to it.value
        }.toMap()

        return CmmnProcessDef.create {
            withId(processDefId)
            withDefinitionsId(element.id)
            withName(Json.mapper.convert(name, MLText::class.java) ?: MLText())
            withEcosType(EntityRef.valueOf(ecosType))
            withCases(cases)
            withArtifacts(artifacts)
            withCmmnDi(cmmnDi)
            withOtherData(ObjectData.create(otherData))
        }
    }

    private fun importArtifact(artifact: TArtifact, context: ImportContext): ArtifactDef {
        val dataWithType = context.converters.import(artifact, context)
        return ArtifactDef(
            artifact.id,
            dataWithType.type,
            dataWithType.data
        )
    }

    private fun importCase(case: Case, context: ImportContext): CaseDef {

        val name = case.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] ?: case.name

        return CaseDef(
            case.id,
            Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            importPlanModel(case.casePlanModel, context)
        )
    }

    private fun importPlanModel(plan: Stage, context: ImportContext): PlanModelDef {
        return PlanModelDef(
            plan.id,
            MLText(plan.name ?: ""),
            plan.exitCriterion?.map {
                context.converters.import(it, ExitCriterionDef::class.java, context).data
            } ?: emptyList(),
            plan.planItem.map {
                context.converters.import(it, ActivityDef::class.java, context).data
            }
        )
    }

    override fun export(element: CmmnProcessDef, context: ExportContext): Definitions {

        val definitions = Definitions()
        definitions.id = element.definitionsId

        element.cases.forEach {
            definitions.case.add(exportCase(it, context))
        }

        definitions.cmmndi = context.converters.export(element.cmmnDi, context)
        definitions.name = MLText.getClosestValue(element.name, I18nContext.getLocale())
        definitions.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(element.name)
        definitions.otherAttributes[CmmnXmlUtils.PROP_ECOS_FORMAT] = CmmnFormat.ECOS_CMMN.code

        element.artifacts.filter {
            it.type != ConvertUtils.getTypeByClass(AssociationDef::class.java)
        }.map {
            definitions.artifact.add(exportArtifact(it, context))
        }
        element.artifacts.filter {
            it.type == ConvertUtils.getTypeByClass(AssociationDef::class.java)
        }.map {
            definitions.artifact.add(exportArtifact(it, context))
        }

        definitions.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE] = element.ecosType.toString()
        definitions.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID] = element.id

        definitions.targetNamespace = "http://bpmn.io/schema/cmmn"

        element.otherData.forEach { key, value ->
            definitions.otherAttributes[QName(CmmnXmlUtils.NS_ECOS, "other_$key")] = value.toString()
        }

        return definitions
    }

    private fun exportArtifact(artifact: ArtifactDef, context: ExportContext): JAXBElement<out TArtifact> {

        val tArtifact = context.converters.export<TArtifact>(artifact.type, artifact.data, context)
        tArtifact.id = artifact.id

        context.cmmnElementsById[artifact.id] = tArtifact
        return context.converters.convertToJaxb(tArtifact)
    }

    private fun exportCase(case: CaseDef, context: ExportContext): Case {

        val resultCase = Case()
        resultCase.id = case.id
        val name = MLText.getClosestValue(case.name, I18nContext.getLocale())
        if (name.isNotBlank()) {
            resultCase.name = name
        }

        context.cmmnElementsById[resultCase.id] = resultCase

        resultCase.caseRoles = CaseRoles()
        resultCase.caseRoles.id = "case-roles"
        val role = Role()
        role.id = "role"
        resultCase.caseRoles.role.add(role)
        context.cmmnElementsById[role.id] = role

        resultCase.casePlanModel = exportPlanModel(case.planModel, context)
        resultCase.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(case.name)

        return resultCase
    }

    private fun exportPlanModel(model: PlanModelDef, context: ExportContext): Stage {

        val casePlanModel = Stage()
        casePlanModel.id = model.id
        casePlanModel.name = MLText.getClosestValue(model.name, I18nContext.getLocale())
        casePlanModel.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(model.name)

        context.cmmnElementsById[casePlanModel.id] = casePlanModel

        model.exitCriteria.forEach {
            val criterion = context.converters.export<TExitCriterion>(it, context)
            casePlanModel.exitCriterion.add(criterion)
            casePlanModel.sentry.add(criterion.sentryRef as Sentry)
        }

        model.children.map { activityDef ->
            val item = context.converters.export<TPlanItem>(activityDef, context)
            casePlanModel.planItem.add(item)
            casePlanModel.planItemDefinition.add(
                context.converters.convertToJaxb(item.definitionRef as TPlanItemDefinition)
            )
            item.entryCriterion?.forEach { addSentry(casePlanModel, it.sentryRef as? Sentry) }
            item.exitCriterion?.forEach { addSentry(casePlanModel, it.sentryRef as? Sentry) }
        }

        fixRefs(casePlanModel, context)

        return casePlanModel
    }

    fun fixRefs(stage: Stage, context: ExportContext) {
        stage.sentry.forEach { fixSentry(it, context) }
        stage.planItemDefinition.mapNotNull {
            it.value as? Stage
        }.forEach {
            fixRefs(it, context)
        }
        stage.planItemDefinition.mapNotNull {
            it.value as? TUserEventListener
        }.forEach { listener ->
            val newRoles = listener.authorizedRoleRefs.map {
                if (it is String) {
                    context.cmmnElementsById[it] ?: error("Element is not found: $it. Listener: ${listener.id}")
                } else {
                    it
                }
            }
            listener.authorizedRoleRefs.clear()
            newRoles.forEach { listener.authorizedRoleRefs.add(it) }
        }
    }

    private fun fixSentry(sentry: Sentry, context: ExportContext) {
        sentry.onPart?.forEach {
            val value = it.value
            if (value is TPlanItemOnPart) {
                val exitCriterionRef = value.exitCriterionRef
                if (exitCriterionRef is String) {
                    if (exitCriterionRef.isNotEmpty()) {
                        value.exitCriterionRef = needCtxElementById(context, exitCriterionRef)
                    } else {
                        value.exitCriterionRef = null
                    }
                }
                val sourceRef = value.sourceRef
                if (sourceRef is String) {
                    if (sourceRef.isNotEmpty()) {
                        value.sourceRef = needCtxElementById(context, sourceRef)
                    } else {
                        value.sourceRef = null
                    }
                }
            } else if (value is TCaseFileItemOnPart) {
                val sourceRef = value.sourceRef
                if (sourceRef is String) {
                    if (sourceRef.isNotEmpty()) {
                        value.sourceRef = needCtxElementById(context, sourceRef)
                    } else {
                        value.sourceRef = null
                    }
                }
            }
        }
    }

    private fun needCtxElementById(context: ExportContext, id: String): Any {
        return context.cmmnElementsById[id]
            ?: error("Element is not found by id: '$id'. \nElements: ${context.cmmnElementsById.keys}")
    }

    private fun addSentry(stage: Stage, sentry: Sentry?) {
        sentry ?: return
        stage.sentry.add(sentry)
    }
}
