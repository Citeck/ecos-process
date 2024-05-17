package ru.citeck.ecos.process.domain.bpmn.io

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.common.generateElementId
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnMutateData
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CamundaDefinitionsConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CamundaDiagramConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.artifact.CamundaAssociationConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.artifact.CamundaTextAnnotationConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram.CamundaEdgeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram.CamundaPlaneConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.diagram.CamundaShapeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.error.CamundaErrorConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.event.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway.CamundaEventBasedGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway.CamundaExclusiveGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway.CamundaInclusiveGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway.CamundaParallelGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.message.CamundaMessageFlowConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.sequence.CamundaSequenceFlowConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.signal.CamundaSignalConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.task.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool.CamundaCollaborationConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool.CamundaLaneConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool.CamundaLaneSetConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.pool.CamundaParticipantConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.process.CamundaProcessConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.process.CamundaSubProcessConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.BpmnDefinitionsConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.BpmnDiagramConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.artifact.BpmnAssociationConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.artifact.BpmnTextAnnotationConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram.BpmnEdgeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram.BpmnPlaneConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.diagram.BpmnShapeConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.error.BpmnErrorConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.event.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway.BpmnEventBasedGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway.BpmnExclusiveGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway.BpmnInclusiveGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.gateway.BpmnParallelGatewayConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.message.BpmnMessageFlowConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.sequence.BpmnSequenceFlowConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.signal.BpmnSignalConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.task.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool.BpmnCollaborationConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool.BpmnLaneConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool.BpmnLaneSetConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.pool.BpmnParticipantConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.process.BpmnProcessConverter
import ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.process.BpmnSubProcessConverter
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState

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
            BpmnSendTaskConverter::class,
            BpmnUserTaskConverter::class,
            BpmnExclusiveGatewayConverter::class,
            BpmnScriptTaskConverter::class,
            BpmnServiceTaskConverter::class,
            BpmnParallelGatewayConverter::class,
            BpmnIntermediateCatchEventConverter::class,
            BpmnIntermediateThrowEventConverter::class,
            BpmnTimerEventDefinitionConverter::class,
            BpmnErrorEventDefinitionConverter::class,
            BpmnTerminateEventDefinitionConverter::class,
            BpmnBoundaryEventConverter::class,
            BpmnTextAnnotationConverter::class,
            BpmnAssociationConverter::class,
            BpmnSubProcessConverter::class,
            BpmnTaskConverter::class,
            BpmnSignalEventDefinitionConverter::class,
            BpmnSignalConverter::class,
            BpmnErrorConverter::class,
            BpmnParticipantConverter::class,
            BpmnCollaborationConverter::class,
            BpmnLaneSetConverter::class,
            BpmnLaneConverter::class,
            BpmnInclusiveGatewayConverter::class,
            BpmnEventBasedGatewayConverter::class,
            BpmnBusinessRuleTaskConverter::class,
            BpmnConditionalEventDefinitionConverter::class,
            BpmnCallActivityTaskConverter::class,
            BpmnMessageFlowConverter::class
        ),
        extensionTypeResolver
    )

    private val ecosCamundaBpmnConverters = EcosOmgConverters(
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
            CamundaSendTaskConverter::class,
            CamundaUserTaskConverter::class,
            CamundaExclusiveGatewayConverter::class,
            CamundaScriptTaskConverter::class,
            CamundaServiceTaskConverter::class,
            CamundaParallelGatewayConverter::class,
            CamundaIntermediateCatchEventConverter::class,
            CamundaIntermediateThrowEventConverter::class,
            CamundaTimerEventDefinitionConverter::class,
            CamundaErrorEventDefinitionConverter::class,
            CamundaTerminateEventDefinitionConverter::class,
            CamundaBoundaryEventConverter::class,
            CamundaTextAnnotationConverter::class,
            CamundaAssociationConverter::class,
            CamundaSubProcessConverter::class,
            CamundaTaskConverter::class,
            CamundaSignalEventDefinitionConverter::class,
            CamundaSignalConverter::class,
            CamundaErrorConverter::class,
            CamundaParticipantConverter::class,
            CamundaCollaborationConverter::class,
            CamundaLaneSetConverter::class,
            CamundaLaneConverter::class,
            CamundaInclusiveGatewayConverter::class,
            CamundaEventBasedGatewayConverter::class,
            CamundaBusinessRuleTaskConverter::class,
            CamundaConditionalEventDefinitionConverter::class,
            CamundaCallActivityTaskConverter::class,
            CamundaMessageFlowConverter::class
        ),
        extensionTypeResolver
    )

    fun importEcosBpmn(definitions: String, validate: Boolean = true): BpmnDefinitionDef {
        return importEcosBpmn(BpmnXmlUtils.readFromString(definitions), validate)
    }

    fun importEcosBpmn(definitions: TDefinitions, validate: Boolean = true): BpmnDefinitionDef {
        return ecosBpmnConverters.import(definitions, BpmnDefinitionDef::class.java, validate).data
    }

    fun exportEcosBpmn(definitions: BpmnDefinitionDef): TDefinitions {
        return ecosBpmnConverters.export(definitions)
    }

    fun exportEcosBpmnToString(definitions: BpmnDefinitionDef): String {
        return BpmnXmlUtils.writeToString(exportEcosBpmn(definitions))
    }

    fun exportCamundaBpmn(definitions: BpmnDefinitionDef): TDefinitions {
        return ecosCamundaBpmnConverters.export(definitions)
    }

    fun exportCamundaBpmnToString(definitions: BpmnDefinitionDef): String {
        return BpmnXmlUtils.writeToString(exportCamundaBpmn(definitions))
    }

    fun generateDefaultDef(defData: BpmnMutateData): TDefinitions {

        with(defData) {
            val defId = generateElementId("Definitions")

            val defaultDef = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                    xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                    xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                    xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0"
                    xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                    id="$defId"
                    targetNamespace="http://bpmn.io/schema/bpmn"
                    exporter="bpmn-js (https://demo.bpmn.io)"
                    exporterVersion="8.2.0">
              <bpmn:process id="$processDefId" isExecutable="true">
                <bpmn:startEvent id="StartEvent_1ew9rff" />
              </bpmn:process>
              <bpmndi:BPMNDiagram id="BPMNDiagram_1">
                <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="$processDefId">
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
            def.otherAttributes[BPMN_PROP_FORM_REF] = formRef.toString()
            def.otherAttributes[BPMN_PROP_ENABLED] = enabled.toString()
            def.otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = autoStartEnabled.toString()
            def.otherAttributes[BPMN_PROP_AUTO_DELETE_ENABLED] = autoDeleteEnabled.toString()
            def.otherAttributes[BPMN_PROP_SECTION_REF] = sectionRef.toString()
            def.otherAttributes[BPMN_PROP_DEF_STATE] = ProcDefRevDataState.CONVERTED.toString()

            return def
        }
    }
}
