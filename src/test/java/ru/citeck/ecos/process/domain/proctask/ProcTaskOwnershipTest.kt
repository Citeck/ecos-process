package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.*
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_GROUP_ORIGINAL
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_TASK_CANDIDATES_USER_ORIGINAL
import ru.citeck.ecos.process.domain.proctask.api.records.*
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ProcTaskOwnershipTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var procTaskService: ProcTaskService

    @Autowired
    private lateinit var authorityService: EcosAuthoritiesApi

    @Autowired
    private lateinit var recordsService: RecordsService

    companion object {
        private const val TEST_HARRY_USER = "harry"

        private val candidateGroups = listOf("GROUP_test", "GROUP_test2")
        private val candidateUsers = listOf(TEST_HARRY_USER, "ron", "hermione")
    }

    @Test
    fun `claim task via records with candidate groups`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        candidateGroups.forEach { taskService.addCandidateGroup(task.id, it) }

        AuthContext.runAsFull(TEST_HARRY_USER, listOf("GROUP_test")) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "$CURRENT_USER_FLAG"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee.getLocalId()).isEqualTo(TEST_HARRY_USER)
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).containsExactlyInAnyOrderElementsOf(candidateGroups)
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `claim task via records with candidate users`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        candidateUsers.forEach { taskService.addCandidateUser(task.id, it) }

        AuthContext.runAsFull(TEST_HARRY_USER, listOf("GROUP_test")) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "$CURRENT_USER_FLAG"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee.getLocalId()).isEqualTo(TEST_HARRY_USER)
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).isEmpty()
        assertThat(updatedTask.candidateUsersOriginal).containsExactlyInAnyOrderElementsOf(candidateUsers)
    }

    @Test
    fun `claim task via records with candidate groups and users`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        candidateUsers.forEach { taskService.addCandidateUser(task.id, it) }
        candidateGroups.forEach { taskService.addCandidateGroup(task.id, it) }

        AuthContext.runAsFull(TEST_HARRY_USER, listOf("GROUP_test")) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "$CURRENT_USER_FLAG"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee.getLocalId()).isEqualTo(TEST_HARRY_USER)
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).containsExactlyInAnyOrderElementsOf(candidateGroups)
        assertThat(updatedTask.candidateUsersOriginal).containsExactlyInAnyOrderElementsOf(candidateUsers)
    }

    @Test
    fun `claim task as not task actor should fail`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_not_harry_group")

        AuthContext.runAsFull(TEST_HARRY_USER, listOf("GROUP_test")) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "$CURRENT_USER_FLAG"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            assertThrows<IllegalStateException> {
                recordsService.mutate(recordAtt)
            }
        }
    }

    @Test
    fun `claim task as not task actor should allow for admins`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_not_harry_group")

        AuthContext.runAsFull(TEST_HARRY_USER, listOf(AuthRole.ADMIN)) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "$CURRENT_USER_FLAG"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee.getLocalId()).isEqualTo(TEST_HARRY_USER)
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).containsExactlyInAnyOrderElementsOf(listOf("GROUP_not_harry_group"))
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `claim task as not task actor should allow for system`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_not_harry_group")

        AuthContext.runAsFull(TEST_HARRY_USER, listOf(AuthRole.SYSTEM)) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "$CURRENT_USER_FLAG"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee.getLocalId()).isEqualTo(TEST_HARRY_USER)
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).containsExactlyInAnyOrderElementsOf(listOf("GROUP_not_harry_group"))
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `unclaim task via records with candidate groups`() {
        val task = taskService.newTask()
        taskService.saveTask(task)
        taskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL, candidateGroups)

        taskService.setAssignee(task.id, TEST_HARRY_USER)

        AuthContext.runAs(TEST_HARRY_USER) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_RELEASE"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(
            authorityService.getAuthorityNames(updatedTask.candidateGroups)
        ).containsExactlyInAnyOrderElementsOf(
            candidateGroups
        )
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).isEmpty()
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `unclaim task via records with candidate users`() {
        val task = taskService.newTask()
        taskService.saveTask(task)
        taskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_USER_ORIGINAL, candidateUsers)

        taskService.setAssignee(task.id, TEST_HARRY_USER)

        AuthContext.runAs(TEST_HARRY_USER) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_RELEASE"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(
            authorityService.getAuthorityNames(updatedTask.candidateUsers)
        ).containsExactlyInAnyOrderElementsOf(
            candidateUsers
        )

        assertThat(updatedTask.candidateGroupsOriginal).isEmpty()
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `unclaim task via records with candidate users and groups`() {
        val task = taskService.newTask()
        taskService.saveTask(task)
        taskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_GROUP_ORIGINAL, candidateGroups)
        taskService.setVariableLocal(task.id, BPMN_TASK_CANDIDATES_USER_ORIGINAL, candidateUsers)

        taskService.setAssignee(task.id, TEST_HARRY_USER)

        AuthContext.runAs(TEST_HARRY_USER) {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_RELEASE"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee).isEqualTo(EntityRef.EMPTY)
        assertThat(
            authorityService.getAuthorityNames(updatedTask.candidateGroups)
        ).containsExactlyInAnyOrderElementsOf(
            candidateGroups
        )
        assertThat(
            authorityService.getAuthorityNames(updatedTask.candidateUsers)
        ).containsExactlyInAnyOrderElementsOf(
            candidateUsers
        )

        assertThat(updatedTask.candidateGroupsOriginal).isEmpty()
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }

    @Test
    fun `reassign task via records`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.setAssignee(task.id, "harry")
        candidateGroups.forEach { taskService.addCandidateGroup(task.id, it) }

        AuthContext.runAs("harry") {
            val recordAtt = RecordAtts(EntityRef.create(AppName.EPROC, ProcTaskRecords.ID, task.id))
            val ownershipAction = ObjectData.create(
                """
                    {
                        "$CHANGE_OWNER_ACTION_ATT": "$CHANGE_OWNER_RECORD_ACTION_CLAIM",
                        "$CHANGE_OWNER_USER_ATT": "ron"
                    }
                """.trimIndent()
            )
            recordAtt.setAtt(CHANGE_OWNER_ATT, ownershipAction)

            recordsService.mutate(recordAtt)
        }

        val updatedTask = procTaskService.getTaskById(task.id) ?: error("Task not found")

        assertThat(updatedTask.assignee.getLocalId()).isEqualTo("ron")
        assertThat(updatedTask.candidateGroups).isEmpty()
        assertThat(updatedTask.candidateUsers).isEmpty()

        assertThat(updatedTask.candidateGroupsOriginal).containsExactlyInAnyOrderElementsOf(candidateGroups)
        assertThat(updatedTask.candidateUsersOriginal).isEmpty()
    }
}
