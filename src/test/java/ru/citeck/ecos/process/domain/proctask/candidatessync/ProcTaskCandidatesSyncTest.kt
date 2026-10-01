package ru.citeck.ecos.process.domain.proctask.candidatessync

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.*

private const val USER_IVAN = "ivan.petrov"
private const val USER_PETR = "petr.ivanov"
private const val USER_HARRY = "harry"

private const val GROUP_MANAGER = "GROUP_manager"
private const val GROUP_DEVELOPER = "GROUP_developer"

private const val USER_TASK_FOLDER = "usertask"

// user task in "recipients from roles" mode with roles "author" and "fin-signer"
private const val PROC_ROLE = "test-user-task-role"
private const val PROC_ROLE_ROLES = "author,fin-signer"

// user task in multi-instance auto mode with role "author"
private const val PROC_MULTI_INSTANCE = "test-user-task-role-multi-instance-parallel"

// user task in manual recipients mode, which still has role "author" in ecos:assignees
private const val PROC_MANUAL_WITH_ROLES = "test-user-task-assign-manual-with-roles"

/**
 * Tests for [ProcTaskCandidatesSyncService] and [ProcTaskCandidatesSyncRecords].
 *
 * Each test works with its own document, so the sync (which acts on every active task of the document)
 * never picks up tasks of other tests.
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskCandidatesSyncTest {

    companion object {
        private val syncActionRef = EntityRef.create(AppName.EPROC, ProcTaskCandidatesSyncRecords.ID, "")
    }

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var runtimeService: RuntimeService

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var procTaskService: ProcTaskService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var syncService: ProcTaskCandidatesSyncService

    @SpyBean
    private lateinit var camundaRoleService: CamundaRoleService

    @SpyBean
    private lateinit var authorityService: EcosAuthoritiesApi

    private lateinit var docRef: EntityRef

    @BeforeEach
    fun setUp() {
        docRef = EntityRef.valueOf("eproc/candidates-sync-doc@${UUID.randomUUID()}")

        helper.saveAndDeployBpmn(USER_TASK_FOLDER, PROC_ROLE)

        // role membership at task creation
        mockRoles(users = listOf(USER_IVAN, USER_PETR), groups = listOf(GROUP_MANAGER, GROUP_DEVELOPER))
    }

    @AfterEach
    fun tearDown() {
        runtimeService.createProcessInstanceQuery()
            .variableValueEquals(BPMN_DOCUMENT_REF, docRef.toString())
            .list()
            .forEach { runtimeService.deleteProcessInstance(it.id, "test cleanup") }

        helper.cleanDefinitions()
        Mockito.reset(camundaRoleService, authorityService)
    }

    @Test
    fun `unassigned task drops members removed from role`() {
        val taskId = startProcess()

        // "author" now resolves only to USER_IVAN, "fin-signer" lost GROUP_DEVELOPER
        mockRoles(users = listOf(USER_IVAN), groups = listOf(GROUP_MANAGER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).containsExactly(taskId)
        assertThat(result.unassignedTaskIds).isEmpty()

        val task = getTask(taskId)
        assertThat(task.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(task.candidateUserNames()).containsExactly(USER_IVAN)
        assertThat(task.candidateGroupNames()).containsExactly(GROUP_MANAGER)
    }

    @Test
    fun `unassigned task gains member added to role`() {
        val taskId = startProcess()

        // USER_HARRY was added to "author", "fin-signer" is unchanged
        mockRoles(
            users = listOf(USER_IVAN, USER_PETR, USER_HARRY),
            groups = listOf(GROUP_MANAGER, GROUP_DEVELOPER)
        )

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).containsExactly(taskId)
        assertThat(result.unassignedTaskIds).isEmpty()

        assertThat(getTask(taskId).candidateUserNames()).containsExactlyInAnyOrder(USER_IVAN, USER_PETR, USER_HARRY)
    }

    @Test
    fun `unassigned task keeps candidate added not from role`() {
        val taskId = startProcess()
        taskService.addCandidateUser(taskId, USER_HARRY)

        // USER_PETR dropped from "author"
        mockRoles(users = listOf(USER_IVAN), groups = listOf(GROUP_MANAGER, GROUP_DEVELOPER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.candidateUserNames()).containsExactlyInAnyOrder(USER_IVAN, USER_HARRY)
        assertThat(task.candidateGroupNames()).containsExactlyInAnyOrder(GROUP_MANAGER, GROUP_DEVELOPER)
    }

    @Test
    fun `unassigns task when current assignee is removed from role`() {
        val taskId = startProcess()
        procTaskService.claimTask(taskId, USER_IVAN)

        // USER_IVAN is no longer an "author", "fin-signer" lost GROUP_MANAGER
        mockRoles(users = listOf(USER_PETR), groups = listOf(GROUP_DEVELOPER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(task.candidateUsersOriginal).isEmpty()
        assertThat(task.candidateUserNames()).containsExactly(USER_PETR)
        assertThat(task.candidateGroupNames()).containsExactly(GROUP_DEVELOPER)
    }

    @Test
    fun `keeps assignee claimed via a still-valid candidate group`() {
        val taskId = startProcess()

        // USER_HARRY is not named by the roles - they claimed the task as a member of GROUP_MANAGER
        procTaskService.claimTask(taskId, USER_HARRY)
        mockUserGroups(USER_HARRY, listOf(GROUP_MANAGER))

        // USER_IVAN dropped from "author", GROUP_DEVELOPER dropped from "fin-signer"
        mockRoles(users = listOf(USER_PETR), groups = listOf(GROUP_MANAGER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).isEmpty()
        assertThat(result.updatedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee.getLocalId()).isEqualTo(USER_HARRY)
        assertThat(task.candidateUsersOriginal).containsExactly(USER_PETR)
        assertThat(task.candidateGroupsOriginal).containsExactly(GROUP_MANAGER)
    }

    @Test
    fun `unassigns task when claimed via a candidate group dropped from role`() {
        val taskId = startProcess()

        // USER_HARRY is not named by the roles - they claimed the task as a member of GROUP_MANAGER
        procTaskService.claimTask(taskId, USER_HARRY)
        mockUserGroups(USER_HARRY, listOf(GROUP_MANAGER))

        // GROUP_MANAGER is no longer resolved by the roles
        mockRoles(users = listOf(USER_PETR), groups = listOf(GROUP_DEVELOPER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(task.candidateUsersOriginal).isEmpty()
        assertThat(task.candidateGroupsOriginal).isEmpty()
        assertThat(task.candidateUserNames()).containsExactly(USER_PETR)
        assertThat(task.candidateGroupNames()).containsExactly(GROUP_DEVELOPER)
    }

    @Test
    fun `keeps assignee set manually outside of role`() {
        val taskId = startProcess()

        // USER_HARRY is not a member of the role - the task was reassigned to them manually
        procTaskService.setAssignee(taskId, USER_HARRY)
        mockUserGroups(USER_HARRY, emptyList())

        // USER_IVAN dropped from "author"
        mockRoles(users = listOf(USER_PETR), groups = listOf(GROUP_MANAGER, GROUP_DEVELOPER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).isEmpty()
        assertThat(result.updatedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee.getLocalId()).isEqualTo(USER_HARRY)
        assertThat(task.candidateUsersOriginal).containsExactly(USER_PETR)
        assertThat(task.candidateGroupsOriginal).containsExactlyInAnyOrder(GROUP_MANAGER, GROUP_DEVELOPER)
    }

    @Test
    fun `auto-assigned sole candidate becomes shared when role gains a member`() {
        // the role resolves to a single user at creation - the task is assigned to them directly,
        // without identity links
        mockRoles(users = listOf(USER_IVAN), groups = emptyList())
        val taskId = startProcess()
        assertThat(getTask(taskId).assignee.getLocalId()).isEqualTo(USER_IVAN)

        // USER_PETR was added to "author"
        mockRoles(users = listOf(USER_IVAN, USER_PETR), groups = emptyList())

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(task.candidateUsersOriginal).isEmpty()
        assertThat(task.candidateUserNames()).containsExactlyInAnyOrder(USER_IVAN, USER_PETR)
    }

    @Test
    fun `auto-assigned sole candidate is left alone when role still resolves to just them`() {
        mockRoles(users = listOf(USER_IVAN), groups = emptyList())
        val taskId = startProcess()

        // role membership is unchanged
        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).isEmpty()
        assertThat(result.unassignedTaskIds).isEmpty()

        assertThat(getTask(taskId).assignee.getLocalId()).isEqualTo(USER_IVAN)
    }

    @Test
    fun `auto-assigned task is reassigned when the only role member is replaced`() {
        mockRoles(users = listOf(USER_IVAN), groups = emptyList())
        val taskId = startProcess()

        // USER_IVAN replaced with USER_PETR
        mockRoles(users = listOf(USER_PETR), groups = emptyList())

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee.getLocalId()).isEqualTo(USER_PETR)
        assertThat(task.candidateUsers).isEmpty()

        // the task is now auto-assigned to USER_PETR - an unchanged role doesn't touch it
        val secondResult = syncService.syncCandidates(docRef)
        assertThat(secondResult.updatedTaskIds).isEmpty()
    }

    @Test
    fun `keeps assignee set manually on auto-assigned task`() {
        mockRoles(users = listOf(USER_IVAN), groups = emptyList())
        val taskId = startProcess()

        // the task auto-assigned to USER_IVAN was reassigned manually to USER_HARRY, who is not in the role
        procTaskService.setAssignee(taskId, USER_HARRY)
        mockUserGroups(USER_HARRY, emptyList())

        // USER_PETR was added to "author"
        mockRoles(users = listOf(USER_IVAN, USER_PETR), groups = emptyList())

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).isEmpty()

        val task = getTask(taskId)
        assertThat(task.assignee.getLocalId()).isEqualTo(USER_HARRY)
        assertThat(task.candidateUsers).isEmpty()
        assertThat(task.candidateUsersOriginal).containsExactlyInAnyOrder(USER_IVAN, USER_PETR)
    }

    @Test
    fun `shared task is assigned to the only user left in role`() {
        val taskId = startProcess()

        // only USER_IVAN is left in the roles
        mockRoles(users = listOf(USER_IVAN), groups = emptyList())

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).containsExactly(taskId)
        assertThat(result.unassignedTaskIds).isEmpty()

        // same as for a task created with a single user recipient
        val task = getTask(taskId)
        assertThat(task.assignee.getLocalId()).isEqualTo(USER_IVAN)
        assertThat(task.candidateUsers).isEmpty()
        assertThat(task.candidateGroups).isEmpty()
    }

    @Test
    fun `claimed task is assigned to the only user left in role`() {
        val taskId = startProcess()
        procTaskService.claimTask(taskId, USER_IVAN)

        // USER_IVAN dropped, only USER_PETR is left in the roles
        mockRoles(users = listOf(USER_PETR), groups = emptyList())

        val result = syncService.syncCandidates(docRef)
        assertThat(result.unassignedTaskIds).containsExactly(taskId)

        val task = getTask(taskId)
        assertThat(task.assignee.getLocalId()).isEqualTo(USER_PETR)
        assertThat(task.candidateUsers).isEmpty()
        assertThat(task.candidateGroups).isEmpty()
        assertThat(task.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `task is left unchanged when roles resolve to nobody`() {
        val taskId = startProcess()

        mockRoles(users = emptyList(), groups = emptyList())

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).isEmpty()

        val task = getTask(taskId)
        assertThat(task.candidateUserNames()).containsExactlyInAnyOrder(USER_IVAN, USER_PETR)
        assertThat(task.candidateGroupNames()).containsExactlyInAnyOrder(GROUP_MANAGER, GROUP_DEVELOPER)
    }

    @Test
    fun `multi instance auto mode tasks are skipped`() {
        helper.saveAndDeployBpmn(USER_TASK_FOLDER, PROC_MULTI_INSTANCE)
        Mockito.doReturn(listOf(USER_IVAN, USER_PETR, GROUP_MANAGER))
            .`when`(camundaRoleService).getAuthorityNames(docRef.toString(), "author")

        startProcess(PROC_MULTI_INSTANCE)
        val tasks = procTaskService.getTasksByDocument(docRef.toString())
        assertThat(tasks).hasSize(3)

        // role membership changed - must not turn the instance tasks into shared ones
        mockRoles(users = listOf(USER_IVAN, USER_PETR, USER_HARRY), groups = emptyList(), roles = "author")

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).isEmpty()
        assertThat(result.unassignedTaskIds).isEmpty()

        val syncedTasks = tasks.map { getTask(it.id) }
        assertThat(syncedTasks.map { it.assignee.getLocalId() })
            .containsExactlyInAnyOrder(USER_IVAN, USER_PETR, "")
        syncedTasks.forEach { assertThat(it.candidateUsers).isEmpty() }

        val groupTask = syncedTasks.single { EntityRef.isEmpty(it.assignee) }
        assertThat(groupTask.candidateGroupNames()).containsExactly(GROUP_MANAGER)
    }

    @Test
    fun `manual recipients mode task is skipped`() {
        helper.saveAndDeployBpmn(USER_TASK_FOLDER, PROC_MANUAL_WITH_ROLES)
        val taskId = startProcess(PROC_MANUAL_WITH_ROLES)

        mockRoles(users = listOf(USER_HARRY), groups = emptyList(), roles = "author")

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).isEmpty()

        val task = getTask(taskId)
        assertThat(task.candidateUserNames()).containsExactlyInAnyOrder(USER_IVAN, USER_PETR)
        assertThat(task.candidateGroupNames()).containsExactlyInAnyOrder(GROUP_MANAGER, GROUP_DEVELOPER)
    }

    @Test
    fun `roles are resolved once per sync call`() {
        // two active tasks of the same document with the same roles
        val firstTaskId = startProcess()
        val secondTaskId = startProcess()

        mockRoles(users = listOf(USER_IVAN), groups = listOf(GROUP_MANAGER))
        Mockito.clearInvocations(camundaRoleService)

        val result = syncService.syncCandidates(docRef)
        assertThat(result.updatedTaskIds).containsExactlyInAnyOrder(firstTaskId, secondTaskId)

        verify(camundaRoleService, times(1)).getUserNames(docRef.toString(), PROC_ROLE_ROLES)
        verify(camundaRoleService, times(1)).getGroupNames(docRef.toString(), PROC_ROLE_ROLES)
    }

    @Test
    fun `failure on one task doesn't stop sync of others`() {
        val failingTaskId = startProcess()
        val otherTaskId = startProcess()

        // the failing task sync fails in the middle: after its stashed candidates are updated,
        // on the check whether the assignee came from the role
        procTaskService.claimTask(failingTaskId, USER_HARRY)
        Mockito.doThrow(RuntimeException("test failure")).`when`(authorityService).getUserAuthorities(USER_HARRY)

        mockRoles(users = listOf(USER_IVAN), groups = listOf(GROUP_MANAGER))

        val result = syncService.syncCandidates(docRef)
        assertThat(result.failedTaskIds).containsExactly(failingTaskId)
        assertThat(result.updatedTaskIds).containsExactly(otherTaskId)

        // the failed task changes are rolled back
        val failedTask = getTask(failingTaskId)
        assertThat(failedTask.assignee.getLocalId()).isEqualTo(USER_HARRY)
        assertThat(failedTask.candidateUsersOriginal).containsExactlyInAnyOrder(USER_IVAN, USER_PETR)
        assertThat(failedTask.candidateGroupsOriginal).containsExactlyInAnyOrder(GROUP_MANAGER, GROUP_DEVELOPER)

        // the other task is synced
        val otherTask = getTask(otherTaskId)
        assertThat(otherTask.candidateUserNames()).containsExactly(USER_IVAN)
        assertThat(otherTask.candidateGroupNames()).containsExactly(GROUP_MANAGER)
    }

    @Test
    fun `sync action - admin call syncs document tasks and returns their ids`() {
        // several tasks of the same document - the result must contain all of them
        val firstTaskId = startProcess()
        val secondTaskId = startProcess()
        procTaskService.claimTask(firstTaskId, USER_PETR)
        procTaskService.claimTask(secondTaskId, USER_PETR)

        // USER_PETR and GROUP_DEVELOPER dropped
        mockRoles(users = listOf(USER_IVAN), groups = listOf(GROUP_MANAGER))

        val result = AuthContext.runAsFull("admin", listOf(AuthRole.ADMIN)) {
            recordsService.mutateAndGetAtts(
                syncActionRef,
                mapOf("documentRef" to docRef.toString()),
                listOf("updatedTaskIds[]", "unassignedTaskIds[]", "failedTaskIds[]")
            )
        }
        assertThat(result.getAtt("updatedTaskIds[]").asStrList())
            .containsExactlyInAnyOrder(firstTaskId, secondTaskId)
        assertThat(result.getAtt("unassignedTaskIds[]").asStrList())
            .containsExactlyInAnyOrder(firstTaskId, secondTaskId)
        assertThat(result.getAtt("failedTaskIds[]").asStrList()).isEmpty()

        listOf(firstTaskId, secondTaskId).forEach { taskId ->
            val task = getTask(taskId)
            assertThat(task.assignee).isEqualTo(EntityRef.EMPTY)
            assertThat(task.candidateUserNames()).containsExactly(USER_IVAN)
            assertThat(task.candidateGroupNames()).containsExactly(GROUP_MANAGER)
        }
    }

    @Test
    fun `sync action - non admin call is rejected`() {
        val taskId = startProcess()

        mockRoles(users = listOf(USER_IVAN), groups = listOf(GROUP_MANAGER))

        assertThrows<Exception> {
            AuthContext.runAsFull(USER_HARRY, listOf(AuthRole.USER)) {
                recordsService.mutate(syncActionRef, mapOf("documentRef" to docRef.toString()))
            }
        }

        // the task is not synced
        val task = getTask(taskId)
        assertThat(task.candidateUserNames()).containsExactlyInAnyOrder(USER_IVAN, USER_PETR)
        assertThat(task.candidateGroupNames()).containsExactlyInAnyOrder(GROUP_MANAGER, GROUP_DEVELOPER)
    }

    @Test
    fun `sync action - call without documentRef fails`() {
        assertThrows<Exception> {
            AuthContext.runAsFull("admin", listOf(AuthRole.ADMIN)) {
                recordsService.mutate(syncActionRef, emptyMap<String, Any>())
            }
        }
    }

    /**
     * Role membership of the test document, both for task creation (roles expression of the task)
     * and for the sync - both resolve roles via CamundaRoleService.
     */
    private fun mockRoles(users: List<String>, groups: List<String>, roles: String = PROC_ROLE_ROLES) {
        Mockito.doReturn(users).`when`(camundaRoleService).getUserNames(docRef.toString(), roles)
        Mockito.doReturn(groups).`when`(camundaRoleService).getGroupNames(docRef.toString(), roles)
    }

    private fun mockUserGroups(userName: String, groups: List<String>) {
        Mockito.doReturn(groups).`when`(authorityService).getUserAuthorities(userName)
    }

    /**
     * @return id of the first task of the started process
     */
    private fun startProcess(procId: String = PROC_ROLE): String {
        val processInstance = runtimeService.startProcessInstanceByKey(
            procId,
            mapOf(
                BPMN_DOCUMENT_REF to docRef.toString(),
                BPMN_DOCUMENT_TYPE to "candidates-sync-doc"
            )
        )
        return taskService.createTaskQuery()
            .processInstanceId(processInstance.id)
            .list()
            .first()
            .id
    }

    private fun getTask(taskId: String): ProcTaskDto {
        return procTaskService.getTaskById(taskId) ?: error("Task not found: $taskId")
    }

    private fun ProcTaskDto.candidateUserNames(): List<String> {
        return authorityService.getAuthorityNames(candidateUsers)
    }

    private fun ProcTaskDto.candidateGroupNames(): List<String> {
        return authorityService.getAuthorityNames(candidateGroups)
    }
}
