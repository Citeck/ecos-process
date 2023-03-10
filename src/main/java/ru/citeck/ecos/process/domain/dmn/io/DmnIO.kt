package ru.citeck.ecos.process.domain.dmn.io

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.dmn.io.convert.camunda.CamundaDmnDefinitionsConverter
import ru.citeck.ecos.process.domain.dmn.io.convert.ecos.DmnDefinitionsConverter
import ru.citeck.ecos.process.domain.dmn.io.xml.DmnXmlUtils
import ru.citeck.ecos.process.domain.dmn.model.ecos.DmnDefinitionDef
import ru.citeck.ecos.process.domain.dmn.model.omg.TDMNElement
import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters

object DmnIO {

    private val extensionTypeResolver = { item: Any ->
        val result: String? = when (item) {
            is TDMNElement -> item.otherAttributes[DMN_PROP_ECOS_DMN_TYPE]
            else -> null
        }
        result
    }

    private val ecosDmnConverters = EcosOmgConverters(
        listOf(
            DmnDefinitionsConverter::class,
        ),
        extensionTypeResolver
    )

    private val ecosCamundaDmnConverters = EcosOmgConverters(
        listOf(
            CamundaDmnDefinitionsConverter::class,
        ),
        extensionTypeResolver
    )

    fun importEcosDmn(definitions: String): DmnDefinitionDef {
        return importEcosDmn(DmnXmlUtils.readFromString(definitions))
    }

    fun importEcosDmn(definitions: TDefinitions): DmnDefinitionDef {
        return ecosDmnConverters.import(definitions, DmnDefinitionDef::class.java).data
    }

    fun exportEcosDmn(definitions: DmnDefinitionDef): TDefinitions {
        return ecosDmnConverters.export(definitions)
    }

    fun exportEcosDmnToString(definitions: DmnDefinitionDef): String {
        return DmnXmlUtils.writeToString(exportEcosDmn(definitions))
    }

    fun exportCamundaDmn(definitions: DmnDefinitionDef): TDefinitions {
        return ecosCamundaDmnConverters.export(definitions)
    }

    fun exportCamundaDmnToString(definitions: DmnDefinitionDef): String {
        return DmnXmlUtils.writeToString(exportCamundaDmn(definitions))
    }

    fun generateDefaultDef(defId: String, name: MLText): TDefinitions {

        val defaultDef = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="https://www.omg.org/spec/DMN/20191111/MODEL/"
                         xmlns:dmndi="https://www.omg.org/spec/DMN/20191111/DMNDI/"
                         xmlns:biodi="http://bpmn.io/schema/dmn/biodi/2.0"
                         xmlns:di="http://www.omg.org/spec/DMN/20180521/DI/"
                         xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0"
                         xmlns:dc="http://www.omg.org/spec/DMN/20180521/DC/" id="Definitions_1w3m4b1" name="DRD"
                         namespace="http://camunda.org/schema/1.0/dmn" xmlns:modeler="http://camunda.org/schema/modeler/1.0"
                         exporter="Camunda Modeler" exporterVersion="5.8.0" modeler:executionPlatform="Camunda Platform"
                         modeler:executionPlatformVersion="7.17.0">
                <decision id="$defId" name="Decision 1">
                    <decisionTable id="DecisionTable_0nkijsr">
                        <input id="Input_1">
                            <inputExpression id="InputExpression_1" typeRef="string">
                                <text></text>
                            </inputExpression>
                        </input>
                        <output id="Output_1" typeRef="string"/>
                    </decisionTable>
                </decision>
                <dmndi:DMNDI>
                    <dmndi:DMNDiagram>
                        <dmndi:DMNShape dmnElementRef="$defId">
                            <dc:Bounds height="80" width="180" x="160" y="100"/>
                        </dmndi:DMNShape>
                    </dmndi:DMNDiagram>
                </dmndi:DMNDI>
            </definitions>

        """.trimIndent()

        val def = DmnXmlUtils.readFromString(defaultDef)
        def.otherAttributes[DMN_PROP_NAME_ML] = Json.mapper.toString(name)
        def.otherAttributes[DMN_PROP_DEF_ID] = defId

        return def
    }
}
