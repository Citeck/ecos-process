package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.camunda.bpm.engine.FormService
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.context.lib.auth.AuthUser
import ru.citeck.ecos.data.sql.repo.find.DbFindRes
import ru.citeck.ecos.model.lib.delegation.service.DelegationService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.process.domain.proctask.converter.toProcTask
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.*
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import kotlin.system.measureTimeMillis

@Service
class ProcTaskServiceImpl(
    private val authoritiesApi: EcosAuthoritiesApi,
    private val camundaTaskService: TaskService,
    private val camundaTaskFormService: FormService,
    private val cacheableTaskConverter: CacheableTaskConverter,
    private val delegationService: DelegationService
) : ProcTaskService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun findTasks(predicate: Predicate): DbFindRes<String> {
        return findTasks(predicate, emptyList(), QueryPage.create {})
    }

    override fun findTasks(
        predicate: Predicate,
        sortBy: List<SortBy>,
        page: QueryPage
    ): DbFindRes<String> {

        val preparedPredicate = PredicateUtils.mapValuePredicates(predicate) {
            if (it.getAttribute() == ProcTaskSqlQueryBuilder.ATT_ACTOR && it.getValue().isTextual()) {
                var actor = it.getValue().asText()
                if (actor == "\$CURRENT") {
                    actor = AuthContext.getCurrentUser()
                }
                val delegations = delegationService.getActiveAuthDelegations(actor, emptyList())
                if (delegations.isNotEmpty()) {
                    val actorsVariants = mutableListOf<Predicate>()
                    actorsVariants.add(it)
                    delegations.forEach { delegation ->
                        val delegationConditions = mutableListOf<Predicate>()
                        delegationConditions.add(
                            Predicates.`in`(
                                ProcTaskSqlQueryBuilder.ATT_ACTORS,
                                delegation.delegatedAuthorities
                            )
                        )
                        if (delegation.delegatedTypes.isNotEmpty()) {
                            delegationConditions.add(
                                Predicates.inVals(ProcTaskSqlQueryBuilder.ATT_DOCUMENT_TYPE, delegation.delegatedTypes)
                            )
                        } else {
                            delegationConditions.add(
                                Predicates.notEmpty(ProcTaskSqlQueryBuilder.ATT_DOCUMENT_TYPE)
                            )
                        }
                        actorsVariants.add(AndPredicate.of(delegationConditions))
                    }
                    OrPredicate.of(actorsVariants)
                } else {
                    it
                }
            } else {
                it
            }
        } ?: VoidPredicate.INSTANCE

        if (predicate is VoidPredicate && !AuthContext.isRunAsSystemOrAdmin()) {
            return DbFindRes.empty()
        }

        return ProcTaskSqlQueryBuilder(authoritiesApi, camundaTaskService)
            .addConditions(preparedPredicate)
            .setPage(page.skipCount, page.maxItems)
            .setSorting(sortBy)
            .selectTasks()
    }

    override fun getTasksByProcess(processId: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processInstanceId(processId)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByProcessForCurrentUser(processId: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByProcessForCurrentUser: processId=$processId "
        }

        return getTasksByProcess(processId).filter {
            it.isCurrentUserTaskActorOrDelegate()
        }
    }

    override fun getTasksByDocument(document: String): List<ProcTaskDto> {
        return camundaTaskService.createTaskQuery()
            .processVariableValueEquals(BPMN_DOCUMENT_REF, document)
            .initializeFormKeys()
            .list()
            .map { it.toProcTask() }
    }

    override fun getTasksByDocumentForCurrentUser(document: String): List<ProcTaskDto> {
        log.debug {
            "getTasksByDocumentForCurrentUser: document=$document"
        }

        return getTasksByDocument(document).filter {
            it.isCurrentUserTaskActorOrDelegate()
        }
    }

    override fun getTaskById(taskId: String): ProcTaskDto? {
        val result: ProcTaskDto?
        val time = measureTimeMillis {
            result = camundaTaskService.createTaskQuery()
                .taskId(taskId)
                .initializeFormKeys()
                .singleResult()
                ?.toProcTask()
        }

        log.trace { "Get Camunda Task by id: time=$time ms" }

        return result
    }

    override fun getTasksByIds(taskIds: List<String>): List<ProcTaskDto?> {
        val result: List<ProcTaskDto?>
        val time = measureTimeMillis {
            result = camundaTaskService.createTaskQuery()
                .taskIdIn(*taskIds.toTypedArray())
                .initializeFormKeys()
                .list()
                .map {
                    val procTask: ProcTaskDto?
                    val time = measureTimeMillis {
                        procTask = it.toProcTask()
                    }

                    log.trace { "Task to procTask: $time ms" }

                    procTask
                }
        }

        log.debug { "Get Camunda Tasks by ids: $time ms" }

        return result
    }

    override fun completeTask(taskId: String, outcome: Outcome, variables: Map<String, Any?>) {

        val currentUser = AuthContext.getCurrentUser()
        val currentAuthorities = AuthContext.getCurrentAuthorities().toSet()

        val task = getTaskById(taskId) ?: error("Task with id '$taskId' doesn't found")
        val completedOnBehalfOf = getCompletedOnBehalfOfValue(task, currentUser, currentAuthorities)

        camundaTaskService.setVariableLocal(taskId, BPMN_TASK_COMPLETED_BY, currentUser)
        if (completedOnBehalfOf.isNotEmpty()) {
            camundaTaskService.setVariableLocal(taskId, BPMN_TASK_COMPLETED_ON_BEHALF_OF, completedOnBehalfOf)
        }

        val completionVariables = variables.toMutableMap()
        completionVariables[outcome.outcomeId()] = outcome.value
        completionVariables[outcome.nameId()] = outcome.name.toString()
        completionVariables[BPMN_LAST_TASK_COMPLETOR] = currentUser

        log.debug { "Complete task: taskId=$taskId, outcome=$outcome, variables=$completionVariables" }

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
        camundaTaskFormService.submitTaskForm(taskId, completionVariables)
    }

    private fun getCompletedOnBehalfOfValue(
        task: ProcTaskDto,
        currentUser: String,
        currentAuthorities: Set<String>
    ): String {

        if (task.assignee.getLocalId() == currentUser || AuthContext.isRunAsSystemOrAdmin()) {
            return ""
        }
        val candidatesRefs = HashSet(task.candidateUsers)
        candidatesRefs.addAll(task.candidateGroups)
        val candidates = authoritiesApi.getAuthorityNames(candidatesRefs.toList())

        if (task.assignee.isEmpty() && candidates.any { currentAuthorities.contains(it) }) {
            return ""
        }
        val permissionDeniedMsg = "Permission denied. You can't complete task ${task.id}"
        val documentType = task.documentType
        if (documentType.isNullOrBlank()) {
            error(permissionDeniedMsg)
        }
        val delegations = delegationService.getActiveAuthDelegations(
            currentUser,
            listOf(documentType)
        )
        val delegation = if (task.assignee.isNotEmpty()) {
            delegations.find { delegation ->
                delegation.delegatedAuthorities.contains(task.assignee.getLocalId())
            }
        } else {
            delegations.find { delegation ->
                candidates.any { delegation.delegatedAuthorities.contains(it) }
            }
        }
        if (delegation == null) {
            error(permissionDeniedMsg)
        }
        return delegation.delegator
    }

    override fun getVariables(taskId: String): Map<String, Any?> {
        return camundaTaskService.getVariables(taskId)
    }

    override fun claimTask(taskId: String, userId: String) {
        camundaTaskService.claim(taskId, userId)
        moveCandidatesToOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    override fun unclaimTask(taskId: String) {
        camundaTaskService.setAssignee(taskId, null)
        returnCandidatesFromOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    override fun setAssignee(taskId: String, userId: String) {
        camundaTaskService.setAssignee(taskId, userId)
        moveCandidatesToOriginalAtts(taskId)

        cacheableTaskConverter.removeFromActualTaskCache(taskId)
    }

    private fun moveCandidatesToOriginalAtts(taskId: String) {
        camundaTaskService.getIdentityLinksForTask(taskId)?.run {
            val (candidateUsers, candidateGroups) = splitToUserGroupCandidates()

            if (candidateUsers.isNotEmpty()) {
                camundaTaskService.setVariableLocal(
                    taskId,
                    BPMN_TASK_CANDIDATES_USER_ORIGINAL,
                    candidateUsers.toList()
                )
            }

            if (candidateGroups.isNotEmpty()) {
                camundaTaskService.setVariableLocal(
                    taskId,
                    BPMN_TASK_CANDIDATES_GROUP_ORIGINAL,
                    candidateGroups.toList()
                )
            }

            log.debug {
                "Move candidates to original attributes: taskId=$taskId, " +
                    "users=$candidateUsers, groups=$candidateGroups"
            }

            candidateUsers.forEach { camundaTaskService.deleteCandidateUser(taskId, it) }
            candidateGroups.forEach { camundaTaskService.deleteCandidateGroup(taskId, it) }
        }
    }

    private fun returnCandidatesFromOriginalAtts(taskId: String) {
        @Suppress("UNCHECKED_CAST")
        val originalCandidateUsers = camundaTaskService.getVariableLocal(
            taskId,
            BPMN_TASK_CANDIDATES_USER_ORIGINAL
        ) as? List<String>? ?: emptyList()

        originalCandidateUsers.forEach {
            camundaTaskService.addCandidateUser(taskId, it)
        }

        @Suppress("UNCHECKED_CAST")
        val originalCandidateGroups = camundaTaskService.getVariableLocal(
            taskId,
            BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
        ) as? List<String>? ?: emptyList()

        originalCandidateGroups.forEach {
            camundaTaskService.addCandidateGroup(taskId, it)
        }

        log.debug {
            "Return candidates from original attributes: taskId=$taskId, " +
                "users=$originalCandidateUsers, groups=$originalCandidateGroups"
        }

        camundaTaskService.removeVariableLocal(taskId, BPMN_TASK_CANDIDATES_USER_ORIGINAL)
        camundaTaskService.removeVariableLocal(taskId, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL)
    }
}
