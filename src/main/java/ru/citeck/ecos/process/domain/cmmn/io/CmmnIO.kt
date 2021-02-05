package ru.citeck.ecos.process.domain.cmmn.io

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.omg.Definitions
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.DefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact.AssociationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact.TextAnnotationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.CmmnDiConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.EdgeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.ShapeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.action.SetStatusConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.ManualActivationRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.RepetitionRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.RequiredRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfDefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.action.AlfSetStatusConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.event.AlfPlanItemOnPartConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.event.AlfSentryConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.records2.RecordRef

object CmmnIO {
    private val extensionTypeResolver = { item: Any ->
        val result: String? = when (item) {
            is DiagramElement -> item.otherAttributes[CmmnXmlUtils.PROP_ECOS_CMMN_TYPE]
            is TCmmnElement -> item.otherAttributes[CmmnXmlUtils.PROP_ECOS_CMMN_TYPE]
            else -> null
        }
        result
    }

    private val otherAttsResolver = { item: Any ->
        (item as? TCmmnElement)?.otherAttributes
    }

    private val ecosCmmnConverters = EcosOmgConverters(listOf(
        DefinitionsConverter::class,
        CmmnDiConverter::class,
        ActivityConverter::class,
        SentryConverter::class,
        StageConverter::class,
        EdgeConverter::class,
        ShapeConverter::class,
        HumanTaskConverter::class,
        ProcessTaskConverter::class,
        TimerEventListenerConverter::class,
        UserEventListenerConverter::class,
        AssociationConverter::class,
        TextAnnotationConverter::class,
        ExitCriterionConverter::class,
        EntryCriterionConverter::class,
        PlanItemOnPartConverter::class,
        CaseFileOnPartConverter::class,
        RepetitionRuleConverter::class,
        ManualActivationRuleConverter::class,
        RequiredRuleConverter::class,
        SetStatusConverter::class
    ), extensionTypeResolver, otherAttsResolver)

    private val ecosAlfCmmnConverters = EcosOmgConverters(ecosCmmnConverters, listOf(
        AlfDefinitionsConverter::class,
        AlfSentryConverter::class,
        AlfPlanItemOnPartConverter::class,
        AlfSetStatusConverter::class
    ), extensionTypeResolver)

    @JvmStatic
    fun importEcosCmmn(definitions: String): CmmnProcessDef {
        return importEcosCmmn(CmmnXmlUtils.readFromString(definitions))
    }

    @JvmStatic
    fun importEcosCmmn(definitions: Definitions): CmmnProcessDef {
        return ecosCmmnConverters.import(definitions, CmmnProcessDef::class.java).data
    }

    @JvmStatic
    fun exportEcosCmmn(procDef: CmmnProcessDef): Definitions {
        return ecosCmmnConverters.export(procDef)
    }

    @JvmStatic
    fun exportEcosCmmnToString(procDef: CmmnProcessDef): String {
        return CmmnXmlUtils.writeToString(exportEcosCmmn(procDef))
    }

    @JvmStatic
    fun exportAlfCmmn(procDef: CmmnProcessDef): Definitions {
        return ecosAlfCmmnConverters.export(procDef)
    }

    @JvmStatic
    fun exportAlfCmmnToString(procDef: CmmnProcessDef): String {
        return CmmnXmlUtils.writeToString(exportAlfCmmn(procDef))
    }

    fun generateDefaultDef(processDefId: String, name: MLText, ecosType: RecordRef): CmmnProcessDef {

        val defaultDef = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cmmn:definitions
                    xmlns:dc="http://www.omg.org/spec/CMMN/20151109/DC"
                    xmlns:cmmndi="http://www.omg.org/spec/CMMN/20151109/CMMNDI"
                    xmlns:cmmn="http://www.omg.org/spec/CMMN/20151109/MODEL"
                    xmlns:ecos="http://www.citeck.ru/ecos/cmmn/1.0"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    id="Definitions_0fet87u"
                    targetNamespace="http://bpmn.io/schema/cmmn"
                    ecos:processDefId="default"
                    exporter="cmmn-js (https://demo.bpmn.io/cmmn)" exporterVersion="0.20.0">
              <cmmn:case id="Case_1bhr1sf">
                <cmmn:casePlanModel id="CasePlanModel_18oeh9b" name="A CasePlanModel">
                  <cmmn:planItem id="PlanItem_1j0scah" definitionRef="HumanTask_0psu33s" />
                  <cmmn:humanTask id="HumanTask_0psu33s" />
                </cmmn:casePlanModel>
                <cmmn:caseRoles>
                    <cmmn:role id="role" />
                </cmmn:caseRoles>
              </cmmn:case>
              <cmmndi:CMMNDI>
                <cmmndi:CMMNDiagram id="CMMNDiagram_1">
                  <cmmndi:Size width="500" height="500" />
                  <cmmndi:CMMNShape id="DI_CasePlanModel_18oeh9b" cmmnElementRef="CasePlanModel_18oeh9b">
                    <dc:Bounds x="156" y="99" width="534" height="389" />
                    <cmmndi:CMMNLabel />
                  </cmmndi:CMMNShape>
                  <cmmndi:CMMNShape id="PlanItem_1j0scah_di" cmmnElementRef="PlanItem_1j0scah">
                    <dc:Bounds x="192" y="132" width="100" height="80" />
                    <cmmndi:CMMNLabel />
                  </cmmndi:CMMNShape>
                </cmmndi:CMMNDiagram>
              </cmmndi:CMMNDI>
            </cmmn:definitions>
        """.trimIndent()

        val cmmnDef = importEcosCmmn(CmmnXmlUtils.readFromString(defaultDef))

        return CmmnProcessDef.create {
            withId(processDefId)
            withDefinitionsId(cmmnDef.definitionsId)
            withName(name)
            withEcosType(ecosType)
            withCases(cmmnDef.cases)
            withArtifacts(cmmnDef.artifacts)
            withCmmnDi(cmmnDef.cmmnDi)
        }
    }
}
