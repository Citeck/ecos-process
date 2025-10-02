package ru.citeck.ecos.process.domain.cmmn.io

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.DefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact.AssociationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.artifact.TextAnnotationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.CmmnDiConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.EdgeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.di.ShapeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.action.SetStatusConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.ManualActivationRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.RepetitionRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.control.RequiredRuleConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecos.plan.event.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.AlfDefinitionsConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.action.AlfSetStatusConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.event.AlfPlanItemOnPartConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.ecosalf.plan.event.AlfSentryConverter
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef
import ru.citeck.ecos.process.domain.cmmn.model.omg.Definitions
import ru.citeck.ecos.process.domain.cmmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class CmmnIO(
    workspaceService: WorkspaceService
) {
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

    private val ecosCmmnConverters = EcosOmgConverters(
        listOf(
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
        ),
        extensionTypeResolver,
        otherAttsResolver,
        workspaceService
    )

    private val ecosAlfCmmnConverters = EcosOmgConverters(
        ecosCmmnConverters,
        listOf(
            AlfDefinitionsConverter::class,
            AlfSentryConverter::class,
            AlfPlanItemOnPartConverter::class,
            AlfSetStatusConverter::class
        ),
        extensionTypeResolver,
        workspaceService = workspaceService
    )

    fun importEcosCmmn(definitions: String): CmmnProcessDef {
        return importEcosCmmn(CmmnXmlUtils.readFromString(definitions))
    }

    fun importEcosCmmn(definitions: Definitions): CmmnProcessDef {
        return ecosCmmnConverters.import(definitions, CmmnProcessDef::class.java).data
    }

    fun exportEcosCmmn(procDef: CmmnProcessDef): Definitions {
        return ecosCmmnConverters.export(procDef)
    }

    fun exportEcosCmmnToString(procDef: CmmnProcessDef): String {
        return CmmnXmlUtils.writeToString(exportEcosCmmn(procDef))
    }

    fun exportAlfCmmn(procDef: CmmnProcessDef): Definitions {
        return ecosAlfCmmnConverters.export(procDef)
    }

    fun exportAlfCmmnToString(procDef: CmmnProcessDef): String {
        return CmmnXmlUtils.writeToString(exportAlfCmmn(procDef))
    }

    fun generateDefaultDef(processDefId: String, name: MLText, ecosType: EntityRef): CmmnProcessDef {

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

    fun generateLegacyDefaultTemplate(processDefId: String, name: MLText, ecosType: EntityRef): Definitions {

        val defaultDef = """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <cmmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:cmmndi="http://www.omg.org/spec/CMMN/20151109/CMMNDI" xmlns:di="http://www.omg.org/spec/CMMN/20151109/DI" xmlns:xs="http://www.w3.org/2001/XMLSchema" xmlns:cmmn="http://www.omg.org/spec/CMMN/20151109/MODEL" xmlns:ecos="http://www.citeck.ru/ecos/cmmn/1.0" xmlns:dc="http://www.omg.org/spec/CMMN/20151109/DC" targetNamespace="http://www.citeck.ru/ecos/case/cmmn/1.0">
                <cmmn:case xmlns:ecosCmmn="http://www.citeck.ru/ecos/case/cmmn/1.0" name="65eac627-f222-4d86-9fdf-d192cbe36b22" id="id-1" ecosCmmn:elementTypes="case-tasks,documents,completeness-levels,subcases,events,case-roles">
                    <cmmn:casePlanModel autoComplete="true" name="Case plan model" id="id-3">
                        <cmmn:planItem definitionRef="id-4" id="id-5">
                            <cmmn:entryCriterion sentryRef="id-7" id="id-8"/>
                            <cmmn:exitCriterion sentryRef="id-10" id="id-11"/>
                        </cmmn:planItem>
                        <cmmn:planItem definitionRef="id-12" id="id-13">
                            <cmmn:entryCriterion sentryRef="id-15" id="id-16"/>
                            <cmmn:entryCriterion sentryRef="id-18" id="id-19"/>
                            <cmmn:exitCriterion sentryRef="id-21" id="id-22"/>
                        </cmmn:planItem>
                        <cmmn:sentry id="id-7" ecosCmmn:originalEvent="case-created">
                            <cmmn:planItemOnPart id="id-6" ecosCmmn:sourceId="id-1" ecosCmmn:nodeType="{http://www.citeck.ru/model/icaseEvent/1.0}caseCreated">
                                <cmmn:standardEvent>create</cmmn:standardEvent>
                            </cmmn:planItemOnPart>
                            <cmmn:ifPart>
                                <cmmn:condition>&lt;!CDATA[&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
            &lt;conditionsList&gt;
                &lt;conditions&gt;
                    &lt;condition&gt;
                        &lt;type xmlns:ns2="http://www.citeck.ru/model/condition/1.0"&gt;ns2:evaluate-script&lt;/type&gt;
                        &lt;properties&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/condition/1.0"&gt;ns2:evaluate-script.script&lt;/type&gt;
                                &lt;value&gt;document.properties['invariants:isDraft'] == true&lt;/value&gt;
                            &lt;/property&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/attribute/1.0"&gt;ns2:isDocument&lt;/type&gt;
                                &lt;value&gt;false&lt;/value&gt;
                            &lt;/property&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/attribute/1.0"&gt;ns2:isContainer&lt;/type&gt;
                                &lt;value&gt;false&lt;/value&gt;
                            &lt;/property&gt;
                        &lt;/properties&gt;
                    &lt;/condition&gt;
                &lt;/conditions&gt;
            &lt;/conditionsList&gt;
            ]]&gt;</cmmn:condition>
                            </cmmn:ifPart>
                        </cmmn:sentry>
                        <cmmn:sentry id="id-10" ecosCmmn:originalEvent="case-properties-changed">
                            <cmmn:planItemOnPart id="id-9" ecosCmmn:sourceId="id-1" ecosCmmn:nodeType="{http://www.citeck.ru/model/icaseEvent/1.0}casePropertiesChanged">
                                <cmmn:standardEvent>resume</cmmn:standardEvent>
                            </cmmn:planItemOnPart>
                            <cmmn:ifPart>
                                <cmmn:condition>&lt;!CDATA[&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
            &lt;conditionsList&gt;
                &lt;conditions&gt;
                    &lt;condition&gt;
                        &lt;type xmlns:ns2="http://www.citeck.ru/model/condition/1.0"&gt;ns2:evaluate-script&lt;/type&gt;
                        &lt;properties&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/condition/1.0"&gt;ns2:evaluate-script.script&lt;/type&gt;
                                &lt;value&gt;document.properties['invariants:isDraft'] != true&lt;/value&gt;
                            &lt;/property&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/attribute/1.0"&gt;ns2:isDocument&lt;/type&gt;
                                &lt;value&gt;false&lt;/value&gt;
                            &lt;/property&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/attribute/1.0"&gt;ns2:isContainer&lt;/type&gt;
                                &lt;value&gt;false&lt;/value&gt;
                            &lt;/property&gt;
                        &lt;/properties&gt;
                    &lt;/condition&gt;
                &lt;/conditions&gt;
            &lt;/conditionsList&gt;
            ]]&gt;</cmmn:condition>
                            </cmmn:ifPart>
                        </cmmn:sentry>
                        <cmmn:sentry id="id-15" ecosCmmn:originalEvent="activity-stopped">
                            <cmmn:planItemOnPart sourceRef="id-5" id="id-14" ecosCmmn:sourceId="id-4" ecosCmmn:title="Черновик" ecosCmmn:nodeType="{http://www.citeck.ru/model/icaseEvent/1.0}activityStoppedEvent">
                                <cmmn:standardEvent>complete</cmmn:standardEvent>
                            </cmmn:planItemOnPart>
                        </cmmn:sentry>
                        <cmmn:sentry id="id-18" ecosCmmn:originalEvent="case-created">
                            <cmmn:planItemOnPart id="id-17" ecosCmmn:sourceId="id-1" ecosCmmn:nodeType="{http://www.citeck.ru/model/icaseEvent/1.0}caseCreated">
                                <cmmn:standardEvent>create</cmmn:standardEvent>
                            </cmmn:planItemOnPart>
                            <cmmn:ifPart>
                                <cmmn:condition>&lt;!CDATA[&lt;?xml version="1.0" encoding="UTF-8" standalone="yes"?&gt;
            &lt;conditionsList&gt;
                &lt;conditions&gt;
                    &lt;condition&gt;
                        &lt;type xmlns:ns2="http://www.citeck.ru/model/condition/1.0"&gt;ns2:evaluate-script&lt;/type&gt;
                        &lt;properties&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/condition/1.0"&gt;ns2:evaluate-script.script&lt;/type&gt;
                                &lt;value&gt;document.properties['invariants:isDraft'] != true&lt;/value&gt;
                            &lt;/property&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/attribute/1.0"&gt;ns2:isDocument&lt;/type&gt;
                                &lt;value&gt;false&lt;/value&gt;
                            &lt;/property&gt;
                            &lt;property&gt;
                                &lt;type xmlns:ns2="http://www.citeck.ru/model/attribute/1.0"&gt;ns2:isContainer&lt;/type&gt;
                                &lt;value&gt;false&lt;/value&gt;
                            &lt;/property&gt;
                        &lt;/properties&gt;
                    &lt;/condition&gt;
                &lt;/conditions&gt;
            &lt;/conditionsList&gt;
            ]]&gt;</cmmn:condition>
                            </cmmn:ifPart>
                        </cmmn:sentry>
                        <cmmn:sentry id="id-21" ecosCmmn:originalEvent="stage-children-stopped">
                            <cmmn:planItemOnPart sourceRef="id-13" id="id-20" ecosCmmn:sourceId="id-12" ecosCmmn:title="Процесс" ecosCmmn:nodeType="{http://www.citeck.ru/model/icaseEvent/1.0}stageChildrenStopped">
                                <cmmn:standardEvent>complete</cmmn:standardEvent>
                            </cmmn:planItemOnPart>
                        </cmmn:sentry>
                        <cmmn:stage xmlns:activ="http://www.citeck.ru/model/activity/1.0" xmlns:invariants="http://www.citeck.ru/model/invariants/1.0" xmlns:attr="http://www.citeck.ru/model/attribute/1.0" xmlns:lifecycle="http://www.citeck.ru/model/lifecycle/1.0" xmlns:stages="http://www.citeck.ru/model/stages/1.0" autoComplete="true" name="13f9aa8b-78c0-4824-ad00-59e4332b891b" id="id-4" activ:actualEndDate="" activ:manualStopped="false" invariants:isDraft="false" activ:manualStarted="false" activ:index="1" activ:autoEvents="false" ecosCmmn:startCompletnessLevels="" attr:isDocument="false" lifecycle:state="Not started" invariants:canReturnToDraft="false" activ:typeVersion="1" activ:plannedStartDate="2021-04-12T10:07:00.000+07:00" ecosCmmn:caseStatus="draft" activ:repeatable="true" attr:isContainer="false" activ:actualStartDate="" ecosCmmn:stopCompletnessLevels="" ecosCmmn:title="Черновик" stages:caseStatusAssoc-prop=""/>
                        <cmmn:stage xmlns:activ="http://www.citeck.ru/model/activity/1.0" xmlns:invariants="http://www.citeck.ru/model/invariants/1.0" xmlns:attr="http://www.citeck.ru/model/attribute/1.0" xmlns:lifecycle="http://www.citeck.ru/model/lifecycle/1.0" xmlns:stages="http://www.citeck.ru/model/stages/1.0" autoComplete="false" name="c788aabd-24b0-488f-a6d6-9e6feff2e568" id="id-12" activ:manualStopped="true" invariants:isDraft="false" activ:manualStarted="true" activ:index="999999" activ:autoEvents="false" ecosCmmn:startCompletnessLevels="" attr:isDocument="false" lifecycle:state="Not started" invariants:canReturnToDraft="false" activ:typeVersion="1" ecosCmmn:caseStatus="new" activ:repeatable="true" attr:isContainer="false" ecosCmmn:stopCompletnessLevels="" ecosCmmn:title="Процесс" stages:caseStatusAssoc-prop=""/>
                    </cmmn:casePlanModel>
                    <cmmn:caseRoles>
                        <cmmn:role name="Инициатор" id="id-2" ecosCmmn:roleVarName="initiator" ecosCmmn:roleAssignees="" ecosCmmn:isReferenceRole="false" ecosCmmn:nodeType="{http://www.citeck.ru/model/icaseRole/1.0}role"/>
                    </cmmn:caseRoles>
                </cmmn:case>
            </cmmn:definitions>
        """.trimIndent()

        val definitions = CmmnXmlUtils.readFromString(defaultDef)

        definitions.otherAttributes[CmmnXmlUtils.PROP_NAME_ML] = Json.mapper.toString(name)
        definitions.otherAttributes[CmmnXmlUtils.PROP_PROCESS_DEF_ID] = processDefId
        definitions.otherAttributes[CmmnXmlUtils.PROP_ECOS_TYPE] = ecosType.toString()

        return definitions
    }
}
