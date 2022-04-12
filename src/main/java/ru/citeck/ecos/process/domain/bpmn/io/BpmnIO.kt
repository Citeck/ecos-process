package ru.citeck.ecos.process.domain.bpmn.io

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CamundaDefinitionsConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CamundaDiagramConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CamundaProcessConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram.CamundaEdgeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram.CamundaPlaneConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram.CamundaShapeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event.CamundaEndEventConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event.CamundaStartEventConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.sequence.CamundaSequenceFlowConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.task.CamundaSendTaskConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.BpmnDefinitionsConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.BpmnDiagramConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.BpmnProcessConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram.BpmnEdgeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram.BpmnPlaneConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram.BpmnShapeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event.BpmnEndEventConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event.BpmnStartEventConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.sequence.BpmnSequenceFlowConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.task.BpmnSendTaskConverter
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters
import ru.citeck.ecos.records2.RecordRef

object BpmnIO {

    private val extensionTypeResolver = { item: Any ->
        val result: String? = when (item) {
            is TBaseElement -> item.otherAttributes[BPMN_PROP_ECOS_BPMN_TYPE]
            else -> null
        }
        result
    }

    private val ecosBpmnConverters = EcosOmgConverters(
        listOf(
            BpmnDefinitionsConverter::class,
            BpmnShapeConverter::class,
            BpmnEdgeConverter::class,
            BpmnPlaneConverter::class,
            BpmnDiagramConverter::class,
            BpmnProcessConverter::class,
            BpmnStartEventConverter::class,
            BpmnEndEventConverter::class,
            BpmnSequenceFlowConverter::class,
            BpmnSendTaskConverter::class
        ), extensionTypeResolver
    )

    private val ecosCamundaConverters = EcosOmgConverters(
        listOf(
            CamundaDefinitionsConverter::class,
            CamundaShapeConverter::class,
            CamundaEdgeConverter::class,
            CamundaPlaneConverter::class,
            CamundaDiagramConverter::class,
            CamundaProcessConverter::class,
            CamundaStartEventConverter::class,
            CamundaEndEventConverter::class,
            CamundaSequenceFlowConverter::class,
            CamundaSendTaskConverter::class
        ), extensionTypeResolver
    )

    fun importEcosBpmn(definitions: String): BpmnDefinitionDef {
        return importEcosBpmn(BpmnXmlUtils.readFromString(definitions))
    }

    fun importEcosBpmn(definitions: TDefinitions): BpmnDefinitionDef {
        return ecosBpmnConverters.import(definitions, BpmnDefinitionDef::class.java).data
    }

    fun exportEcosBpmn(definitions: BpmnDefinitionDef): TDefinitions {
        return ecosBpmnConverters.export(definitions)
    }

    fun exportEcosBpmnToString(definitions: BpmnDefinitionDef): String {
        return BpmnXmlUtils.writeToString(exportEcosBpmn(definitions))
    }

    fun exportCamundaBpmn(definitions: BpmnDefinitionDef): TDefinitions {
        return ecosCamundaConverters.export(definitions)
    }

    fun exportCamundaBpmnToString(definitions: BpmnDefinitionDef): String {
        return BpmnXmlUtils.writeToString(exportCamundaBpmn(definitions))
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
                    xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                    id="Definitions_0hq0c8n"
                    targetNamespace="http://bpmn.io/schema/bpmn"
                    exporter="bpmn-js (https://demo.bpmn.io)"
                    exporterVersion="8.2.0">
              <bpmn:process id="Process_0ib6j41" isExecutable="true">
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

        val def = BpmnXmlUtils.readFromString(defaultDef)
        def.otherAttributes[BPMN_PROP_ECOS_TYPE] = ecosType.toString()
        def.otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(name)
        def.otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = processDefId

        return def
    }
}
