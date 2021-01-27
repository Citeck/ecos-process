package ru.citeck.ecos.process.domain.ecmmn.io

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.cmmn.model.Definitions
import ru.citeck.ecos.process.domain.cmmn.model.ObjectFactory
import ru.citeck.ecos.process.domain.cmmn.service.CmmnUtils
import ru.citeck.ecos.process.domain.ecmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.ecmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.ecmmn.io.convert.CmmnConverters
import ru.citeck.ecos.process.domain.ecmmn.io.convert.DefinitionsConverter
import ru.citeck.ecos.process.domain.ecmmn.model.CmmnProcDef
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.CmmnCaseDef
import ru.citeck.ecos.process.domain.ecmmn.model.casemodel.plan.CmmnPlanModelDef
import ru.citeck.ecos.process.domain.ecmmn.model.di.CmmnDiDef
import ru.citeck.ecos.process.domain.ecmmn.model.di.diagram.CmmnDiagram
import ru.citeck.ecos.records2.RecordRef

object CmmnIO {

    private val objectFactory = ObjectFactory()
    private val converters = CmmnConverters(objectFactory)

    fun import(definitions: String): CmmnProcDef {
        return import(CmmnUtils.readFromString(definitions))
    }

    fun import(definitions: Definitions): CmmnProcDef {
        return converters.import(definitions, CmmnProcDef::class.java, ImportContext()).data
    }

    fun export(procDef: CmmnProcDef): Definitions {
        return converters.export(DefinitionsConverter.TYPE, procDef, ExportContext())
    }

    fun exportToString(procDef: CmmnProcDef): String {
        return CmmnUtils.writeToString(export(procDef))
    }

    fun generateDefaultDef(artifactId: String, name: MLText, ecosType: RecordRef): CmmnProcDef {

        val defaultDef = """
            <?xml version="1.0" encoding="UTF-8"?>
            <cmmn:definitions xmlns:dc="http://www.omg.org/spec/CMMN/20151109/DC" xmlns:cmmndi="http://www.omg.org/spec/CMMN/20151109/CMMNDI" xmlns:cmmn="http://www.omg.org/spec/CMMN/20151109/MODEL" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="Definitions_0fet87u" targetNamespace="http://bpmn.io/schema/cmmn" exporter="cmmn-js (https://demo.bpmn.io/cmmn)" exporterVersion="0.20.0">
              <cmmn:case id="Case_1bhr1sf">
                <cmmn:casePlanModel id="CasePlanModel_18oeh9b" name="A CasePlanModel">
                  <cmmn:planItem id="PlanItem_1j0scah" definitionRef="HumanTask_0psu33s" />
                  <cmmn:humanTask id="HumanTask_0psu33s" />
                </cmmn:casePlanModel>
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

        val cmmnDef = import(CmmnUtils.readFromString(defaultDef))

        return CmmnProcDef(
            artifactId,
            name,
            ecosType,
            cmmnDef.cases,
            cmmnDef.artifacts,
            cmmnDef.cmmnDi
        )
    }
}
