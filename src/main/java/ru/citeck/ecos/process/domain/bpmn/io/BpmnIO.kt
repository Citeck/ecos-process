package ru.citeck.ecos.process.domain.bpmn.io

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.DefinitionsConverter
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.Definitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters
import ru.citeck.ecos.records2.RecordRef

object BpmnIO {

    private val extensionTypeResolver = { item: Any ->
        val result: String? = when (item) {
            is TBaseElement -> item.otherAttributes[BpmnXmlUtils.PROP_ECOS_BPMN_TYPE]
            else -> null
        }
        result
    }

    private val ecosBpmnConverters = EcosOmgConverters(listOf(
        DefinitionsConverter::class
    ), extensionTypeResolver)

    @JvmStatic
    fun importEcosBpmn(definitions: String): BpmnProcessDef {
        return importEcosBpmn(BpmnXmlUtils.readFromString(definitions))
    }

    @JvmStatic
    fun importEcosBpmn(definitions: TDefinitions): BpmnProcessDef {
        return ecosBpmnConverters.import(definitions, BpmnProcessDef::class.java).data
    }

    @JvmStatic
    fun exportEcosBpmn(procDef: BpmnProcessDef): Definitions {
        return ecosBpmnConverters.export(procDef)
    }

    @JvmStatic
    fun exportEcosBpmnToString(procDef: BpmnProcessDef): String {
        return CmmnXmlUtils.writeToString(exportEcosBpmn(procDef))
    }

    //todo: replace return value by BpmnProcessDef
    @JvmStatic
    fun generateDefaultDef(processDefId: String, name: MLText, ecosType: RecordRef): TDefinitions {

        val defaultDef = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                    xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                    xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0"
                    id="Definitions_0hq0c8n"
                    targetNamespace="http://bpmn.io/schema/bpmn"
                    exporter="bpmn-js (https://demo.bpmn.io)"
                    exporterVersion="8.2.0">
              <bpmn:process id="Process_0ib6j41" isExecutable="false">
                <bpmn:startEvent id="StartEvent_1ew9rff" />
              </bpmn:process>
              <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_0ib6j41">
                  <bpmndi:BPMNShape id="_BPMNShape_StartEvent_2" bpmnElement="StartEvent_1ew9rff">
                    <dc:Bounds x="156" y="81" width="36" height="36" />
                  </bpmndi:BPMNShape>
                </bpmndi:BPMNPlane>
              </bpmndi:BPMNDiagram>
            </bpmn:definitions>
        """.trimIndent()

        return BpmnXmlUtils.readFromString(defaultDef)
    }
}
