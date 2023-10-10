package ru.citeck.ecos.process.domain.bpmnreport.service

import org.apache.commons.lang3.LocaleUtils
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.sequence.BpmnSequenceFlowDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnLaneSetDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmnreport.model.*
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnProcessReportService(
    val reportElementsService: ReportElementsService
) {

    fun generateReportElementListForBpmnDefinition(bpmnDefinition: BpmnDefinitionDef): List<ReportElement> {
        val ecosType = bpmnDefinition.ecosType
        val flowElements = bpmnDefinition.process.first().flowElements
        val artifacts = bpmnDefinition.process.first().artifacts
        val laneSets = bpmnDefinition.process.first().lanes

        val listElements = ArrayList<ReportElement>()

        listElements.addAll(convertBpmnFlowElementToReportElements(flowElements, ecosType))
        addAnnotationsToReportElements(listElements, artifacts)
        addLanesToReportElements(listElements, laneSets)

        listElements.sortBy { it.number }

        return listElements
    }

    private fun addAnnotationsToReportElements(
        elements: List<ReportElement>,
        artifacts: List<BpmnArtifactDef>
    ) {
        artifacts.filter { it.type == "bpmn:BpmnTextAnnotation" }
            .forEach { annotation ->
                val text = ReportAnnotationElement(
                    Json.mapper.convert(annotation.data["text"], MLText::class.java) ?: MLText()
                )

                val elementId = artifacts
                    .find { it.type == "bpmn:BpmnAssociation" && it.data["targetRef"].asText() == annotation.id }
                    ?.data?.get("sourceRef")?.asText()

                if (elementId != null) {
                    val element = elements.find { it.id == elementId }
                    if (element != null) {
                        if (element.annotations == null) element.annotations = ArrayList()
                        element.annotations?.add(text)
                    }
                }
            }
    }

    private fun addLanesToReportElements(
        elements: List<ReportElement>,
        laneSets: List<BpmnLaneSetDef>
    ) {
        for (laneSet in laneSets) {
            for (lane in laneSet.lanes) {
                val laneElement = ReportLaneElement(lane.name, lane.documentation)

                fun addLaneToElements(elementNames: List<String>) {
                    elementNames.forEach { elementId ->
                        elements.find { it.id == elementId }?.let { element ->
                            element.lane = laneElement
                            element.subProcessElement?.elements?.let {
                                addLaneToElements(element.subProcessElement?.elements!!)
                            }
                        }
                    }
                }

                addLaneToElements(lane.flowRefs)
            }
        }
    }

    private fun convertBpmnFlowElementToReportElements(
        flowElements: List<BpmnFlowElementDef>,
        ecosType: EntityRef?
    ): List<ReportElement> {
        val elements = ArrayList<ReportElement>()

        for (flowElement in flowElements) {

            val elementType = ElementType.values().find { it.flowElementType == flowElement.type } ?: continue

            val reportElement = ReportElement(
                flowElement.id,
                flowElement.data["number"].takeIf { it.isNotNull() }?.asInt()
            )

            when (elementType) {
                //Status
                ElementType.STATUS -> {
                    reportElement.statusElement =
                        reportElementsService.convertReportStatusElement(flowElement, ecosType!!)
                    reportElement.incoming = getReportSequencesForFlowElement(flowElement, flowElements)
                }

                //Gateway
                ElementType.EXCLUSIVE_GATEWAY, ElementType.PARALLEL_GATEWAY,
                ElementType.INCLUSIVE_GATEWAY, ElementType.EVENT_BASED_GATEWAY -> {
                    reportElement.gatewayElement =
                        reportElementsService.convertReportGatewayElement(flowElement, elementType)
                    reportElement.incoming = getReportSequencesForFlowElement(flowElement, flowElements)
                }

                //Event
                ElementType.START_EVENT, ElementType.END_EVENT,
                ElementType.INTERMEDIATE_CATCH_EVENT, ElementType.INTERMEDIATE_THROW_EVENT,
                ElementType.BOUNDARY_EVENT -> {
                    reportElement.eventElement =
                        reportElementsService.convertReportEventElement(flowElement, elementType)
                    reportElement.incoming = getReportSequencesForFlowElement(flowElement, flowElements)
                }

                //Task
                ElementType.USER_TASK, ElementType.SCRIPT_TASK,
                ElementType.SEND_TASK, ElementType.BUSINESS_RULE_TASK -> {
                    reportElement.taskElement =
                        reportElementsService.convertReportTaskElement(flowElement, elementType, ecosType)
                    reportElement.incoming = getReportSequencesForFlowElement(flowElement, flowElements)
                }

                //SubProcess
                ElementType.SUB_PROCESS -> {
                    reportElement.subProcessElement =
                        reportElementsService.convertReportSubProcessElement(flowElement, elementType)

                    val subProcessElements = flowElement.data["flowElements"].asList(BpmnFlowElementDef::class.java)
                    val subProcessArtifacts = flowElement.data["artifacts"].asList(BpmnArtifactDef::class.java)

                    val elementsFromSubProcess = convertBpmnFlowElementToReportElements(subProcessElements, ecosType)
                    if (elementsFromSubProcess.isNotEmpty()) {
                        addAnnotationsToReportElements(elementsFromSubProcess, subProcessArtifacts)
                        reportElement.subProcessElement?.elements = elementsFromSubProcess.map { it.id }

                        elementsFromSubProcess.forEach {
                            it.subProcessElement = ReportSubProcessElement().apply {
                                type = reportElement.subProcessElement?.type ?: ""
                                name = reportElement.subProcessElement?.name ?: MLText()
                            }
                        }

                        elements.addAll(elementsFromSubProcess)
                    }

                    reportElement.incoming = getReportSequencesForFlowElement(flowElement, flowElements)
                }

                //CallActivity
                ElementType.CALL_ACTIVITY -> {
                    reportElement.subProcessElement =
                        reportElementsService.convertReportCallActivityElement(flowElement, elementType)
                    reportElement.incoming = getReportSequencesForFlowElement(flowElement, flowElements)
                }
            }

            elements.add(reportElement)
        }

        return elements
    }

    private fun getReportSequencesForFlowElement(
        flowElement: BpmnFlowElementDef,
        flowElements: List<BpmnFlowElementDef>
    ): List<ReportSequenceElement>? {

        val result = ArrayList<ReportSequenceElement>()

        val incomingSequences = flowElement.data["incoming"].asList(String::class.java)

        for (incomingSequenceId in incomingSequences) {

            val incomingSequence =
                flowElements.find { it.type == "bpmn:BpmnSequenceFlow" && it.id == incomingSequenceId } ?: continue

            val sequence = Json.mapper.convert(incomingSequence.data, BpmnSequenceFlowDef::class.java)
            if (sequence == null || sequence.name.getValues().isEmpty()) continue

            val reportSequenceElement = ReportSequenceElement()
            reportSequenceElement.name = sequence.name

            reportSequenceElement.type = when (sequence.condition.type) {
                ConditionType.OUTCOME -> {

                    val userTask = flowElements
                        .find {
                            it.type == ElementType.USER_TASK.flowElementType &&
                                it.id == sequence.condition.config.outcome.taskDefinitionKey
                        }

                    val taskName = Json.mapper.convert(userTask?.data?.get("name"), MLText::class.java) ?: MLText()
                    val taskOutcomes = userTask?.data?.get("outcomes")?.asList(TaskOutcome::class.java)
                    val taskOutcome =
                        taskOutcomes?.find { it.id == sequence.condition.config.outcome.value }?.name ?: MLText()

                    val mergedMap = (taskName.getValues().entries + taskOutcome.getValues().entries)
                        .groupBy({ it.key }, { it.value })
                        .mapValues { (_, values) -> values.joinToString(" - ") }

                    reportSequenceElement.outcome = MLText(mergedMap)

                    MLText(
                        LocaleUtils.toLocale("en") to "Outcome",
                        LocaleUtils.toLocale("ru") to "Исходящий"
                    )
                }

                ConditionType.SCRIPT -> MLText(
                    LocaleUtils.toLocale("en") to "Script",
                    LocaleUtils.toLocale("ru") to "Скрипт"
                )

                ConditionType.EXPRESSION -> MLText(
                    LocaleUtils.toLocale("en") to "Expression",
                    LocaleUtils.toLocale("ru") to "Выражение"
                )

                else -> null
            }

            result.add(reportSequenceElement)
        }

        return result.takeIf { it.isNotEmpty() }
    }

}
