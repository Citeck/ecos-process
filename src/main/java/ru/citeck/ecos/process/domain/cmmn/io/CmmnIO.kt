package ru.citeck.ecos.process.domain.cmmn.io

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.omg.Definitions
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.EcosOmgConverters
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.DefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact.AssociationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact.TextAnnotationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.CmmnDiConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.EdgeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.ShapeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.ManualActivationRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.RepetitionRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.RequiredRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfDefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfPlanItemOnPartConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfSentryConverter
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.records2.RecordRef
import javax.xml.namespace.QName

object CmmnIO {

    private val PROP_EXT_CMMN_TYPE = QName(CmmnXmlUtils.NS_ECOS, "cmmnType")

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
        RequiredRuleConverter::class
    )) { item -> when (item) {
        is DiagramElement -> item.otherAttributes[PROP_EXT_CMMN_TYPE]
        is TCmmnElement -> item.otherAttributes[PROP_EXT_CMMN_TYPE]
        else -> null
    }}

    private val ecosAlfCmmnConverters = EcosOmgConverters(ecosCmmnConverters, listOf(
        AlfDefinitionsConverter::class,
        AlfSentryConverter::class,
        AlfPlanItemOnPartConverter::class
    )) { null }

    @JvmStatic
    fun importEcosCmmn(definitions: String): CmmnProcDef {
        return importEcosCmmn(CmmnXmlUtils.readFromString(definitions))
    }

    @JvmStatic
    fun importEcosCmmn(definitions: Definitions): CmmnProcDef {
        return ecosCmmnConverters.import(definitions, CmmnProcDef::class.java).data
    }

    @JvmStatic
    fun exportEcosCmmn(procDef: CmmnProcDef): Definitions {
        return ecosCmmnConverters.export(DefinitionsConverter.TYPE, procDef)
    }

    @JvmStatic
    fun exportEcosCmmnToString(procDef: CmmnProcDef): String {
        return CmmnXmlUtils.writeToString(exportEcosCmmn(procDef))
    }

    @JvmStatic
    fun exportAlfCmmn(procDef: CmmnProcDef): Definitions {
        return ecosAlfCmmnConverters.export(DefinitionsConverter.TYPE, procDef)
    }

    @JvmStatic
    fun exportAlfCmmnToString(procDef: CmmnProcDef): String {
        return CmmnXmlUtils.writeToString(exportAlfCmmn(procDef))
    }

    fun generateDefaultDef(processDefId: String, name: MLText, ecosType: RecordRef): CmmnProcDef {

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

        return CmmnProcDef(
            processDefId,
            cmmnDef.definitionsId,
            name,
            ecosType,
            cmmnDef.cases,
            cmmnDef.artifacts,
            cmmnDef.cmmnDi
        )
    }
}
