package ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.utils.StringUtils
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.ConvertUtils
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.DefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.EntryCriterionDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.SentryDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.OnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemOnPartDef
import ru.citeck.ecos.process.domain.cmmn.model.ecos.casemodel.plan.event.onpart.PlanItemTransitionEnum
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import javax.xml.namespace.QName

class AlfDefinitionsConverter : EcosOmgConverter<CmmnProcessDef, Definitions> {

    companion object {

        private val PROP_ELEMENT_TYPES = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "elementTypes")
        private const val PROP_ELEMENT_TYPES_VALUE = "case-tasks,supplementary-files,documents," +
                                                     "completeness-levels,subcases,events,case-roles"

        private val PROP_ALF_ECOS_TYPE = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "caseEcosType")
        private val PROP_ALF_ECOS_KIND = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "caseEcosKind")

        val PROP_NODE_TYPE = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "nodeType")
        private val PROP_TITLE = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "title")

        private val PROP_SOURCE_ID = QName(CmmnXmlUtils.NS_ALF_ECOS_CMMN, "sourceId")
    }

    private val standardConverter = DefinitionsConverter()

    override fun import(element: Definitions, context: ImportContext): CmmnProcessDef {
        error("Import is not supported")
    }

    override fun export(element: CmmnProcessDef, context: ExportContext): Definitions {

        val definitions = standardConverter.export(element, context)
        definitions.targetNamespace = "http://www.citeck.ru/ecos/case/cmmn/1.0"
        definitions.otherAttributes.clear()

        val case = definitions.case[0]
        case.otherAttributes[PROP_ELEMENT_TYPES] = PROP_ELEMENT_TYPES_VALUE

        val firstSlashIdx = element.id.indexOf('/')
        if (firstSlashIdx > 0) {
            case.otherAttributes[PROP_ALF_ECOS_TYPE] = element.id.substring(0, firstSlashIdx)
            case.otherAttributes[PROP_ALF_ECOS_KIND] = element.id.substring(firstSlashIdx + 1)
        } else {
            case.otherAttributes[PROP_ALF_ECOS_TYPE] = element.id
        }

        processStages(case, true, case.casePlanModel, context)

        // is old roles will be used?
        case.caseRoles.role.forEach { it.name = it.id }

        standardConverter.fixRefs(case.casePlanModel, context)
        processSentries(case.casePlanModel)

        return definitions
    }

    private fun processSentries(stage: Stage) {

        stage.sentry?.forEach { sentry ->

            if (sentry.onPart?.size ?: 0 == 0) {
                return@forEach
            }
            sentry.onPart.map { it.value }.forEach {

                val planItemOnPart = it as? TPlanItemOnPart
                val sourceDef = (planItemOnPart
                    ?.sourceRef as? TPlanItem)
                    ?.definitionRef as? TPlanItemDefinition

                if (sourceDef != null) {
                    planItemOnPart.otherAttributes[PROP_SOURCE_ID] = sourceDef.id
                }
            }
        }

        stage.planItemDefinition.mapNotNull { it.value as? Stage }.forEach {
            processSentries(it)
        }
    }

    private fun processStages(parentElement: TCmmnElement, isRoot: Boolean, stage: Stage, context: ExportContext) {

        stage.planItem.forEach { planItem ->

            // add on-parent-create sentry
            if (planItem.entryCriterion == null || planItem.entryCriterion.isEmpty()) {

                val transition: PlanItemTransitionEnum
                val sourceRef: String
                if (isRoot) {
                    transition = PlanItemTransitionEnum.CREATE
                    sourceRef = ""
                } else {
                    transition = PlanItemTransitionEnum.START
                    sourceRef = context.planItemByDefId[parentElement.id]?.id
                        ?: error("PlanItem is not found for ${parentElement.id}")
                }

                val entryOnParentCreateDef = EntryCriterionDef(
                    CmmnXmlUtils.generateId("EntryCriterion"),
                    MLText(),
                    SentryDef(
                        CmmnXmlUtils.generateId("Sentry"),
                        listOf(
                            OnPartDef(
                                ConvertUtils.getTypeByClass(PlanItemOnPartDef::class.java),
                                ObjectData.create(PlanItemOnPartDef(
                                    CmmnXmlUtils.generateId("PlanItemOnPart"),
                                    MLText(),
                                    sourceRef,
                                    transition,
                                    null
                                ))
                            )
                        ),
                        null
                    )
                )

                val entryOnParentCreate = context.converters.export<TEntryCriterion>(entryOnParentCreateDef, context)

                if (isRoot) {
                    (entryOnParentCreate.sentryRef as? Sentry)?.onPart?.mapNotNull {
                        it.value as? TPlanItemOnPart
                    }?.forEach {
                        it.otherAttributes[PROP_SOURCE_ID] = parentElement.id
                    }
                }

                stage.sentry.add(entryOnParentCreate.sentryRef as Sentry)
                planItem.entryCriterion.add(entryOnParentCreate)
            }
            val definitionRef = planItem.definitionRef
            if (definitionRef is Stage) {
                processStages(definitionRef, false, definitionRef, context)
            }
        }

        stage.planItemDefinition.forEach {
            if (StringUtils.isNotBlank(it.value.name)) {
                it.value.otherAttributes[PROP_TITLE] = it.value.name
            }
        }
    }
}
