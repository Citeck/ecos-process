package ru.citeck.ecos.process.domain.bpmn.engine.camunda.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.camunda.bpm.engine.RuntimeService
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.patch.PatchExecutionState
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatchDependsOnApps
import ru.citeck.ecos.webapp.lib.patch.executor.bean.StatefulEcosPatch

@Component
@EcosPatch("bpmn-task-add-document-type-to-existing-tasks", "2023-05-16T00:00:01Z")
@EcosPatchDependsOnApps(AppName.EMODEL)
class BpmnTaskAddDocumentTypeToExistingTasksPatch(
    private val camundaRuntimeService: RuntimeService,
    private val taskService: ProcTaskService,
    private val recordsService: RecordsService
) : StatefulEcosPatch<ObjectData> {

    companion object {
        private val log = KotlinLogging.logger {}

        private const val STATE_ITERATIONS = "iterations"
        private const val STATE_PROCESSED = "processed"
        private const val ITERATION_MAX_ITEMS = 100
    }

    override fun execute(state: ObjectData): PatchExecutionState<ObjectData> {

        log.info { "Iteration processing started. State: $state" }

        val processed = state.get(STATE_PROCESSED, 0)
        val iterations = state.get(STATE_ITERATIONS, 0) + 1

        val tasksWithoutType = taskService.findTasks(
            Predicates.and(
                Predicates.notEmpty(BPMN_DOCUMENT),
                Predicates.empty(BPMN_DOCUMENT_TYPE)
            ),
            emptyList(),
            QueryPage.create { withMaxItems(ITERATION_MAX_ITEMS) }
        ).entities

        log.info { "Found ${tasksWithoutType.size} tasks for migration" }

        if (tasksWithoutType.isNotEmpty()) {

            val tasksDto = taskService.getTasksByIds(tasksWithoutType).filterNotNull()
            val processedProcessInstances = mutableSetOf<String>()

            for (task in tasksDto) {
                val processId = task.processInstanceId.getLocalId()
                if (!processedProcessInstances.add(processId)) {
                    continue
                }
                val variables = camundaRuntimeService.getVariables(processId)
                val docTypeBefore = variables[BPMN_DOCUMENT_TYPE] as? String

                if (!docTypeBefore.isNullOrBlank()) {
                    log.info { "Document type already exists in process $processId. Type: $docTypeBefore" }
                    continue
                }

                val documentRef = (variables[BPMN_DOCUMENT_REF] as? String).toEntityRef()
                val documentType = recordsService.getAtt(
                    documentRef,
                    RecordConstants.ATT_TYPE + ScalarType.LOCAL_ID_SCHEMA
                ).asText().ifBlank { "base" }

                log.info { "Update process $processId with record $documentRef and document type '$documentType'" }
                camundaRuntimeService.setVariable(processId, BPMN_DOCUMENT_TYPE, documentType)
            }
        }

        val newState = ObjectData.create()
            .set(STATE_ITERATIONS, iterations)
            .set(STATE_PROCESSED, processed + tasksWithoutType.size)

        log.info { "Iteration processing completed. New state: $newState" }

        return PatchExecutionState(
            newState,
            iterations > 1000 || tasksWithoutType.size < ITERATION_MAX_ITEMS
        )
    }
}
