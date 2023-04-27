package ru.citeck.ecos.process.domain.proctask

import org.assertj.core.api.Assertions.*
import org.camunda.bpm.engine.TaskService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.proctask.api.records.*
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ProcTaskOwnershipTest {

    @Autowired
    private lateinit var taskService: TaskService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Test
    fun `claim task via records`() {
        val testHarryUser = "harry_claim"


        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_test")

        AuthContext.runAsFull(testHarryUser, listOf("GROUP_test")) {
            val recordAtt = RecordAtts(EntityRef.create("eproc", ProcTaskRecords.ID, task.id))
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

        val taskAssignee = taskService.createTaskQuery().taskId(task.id).singleResult().assignee

        assertThat(taskAssignee).isEqualTo(testHarryUser)
    }


    @Test
    fun `claim task as not task actor should fail`() {
        val testHarryUser = "harry_claim"


        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_not_harry_group")

        AuthContext.runAsFull(testHarryUser, listOf("GROUP_test")) {
            val recordAtt = RecordAtts(EntityRef.create("eproc", ProcTaskRecords.ID, task.id))
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
        val testHarryUser = "harry_claim"


        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_not_harry_group")

        AuthContext.runAsFull(testHarryUser, listOf(AuthRole.ADMIN)) {
            val recordAtt = RecordAtts(EntityRef.create("eproc", ProcTaskRecords.ID, task.id))
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

        val taskAssignee = taskService.createTaskQuery().taskId(task.id).singleResult().assignee

        assertThat(taskAssignee).isEqualTo(testHarryUser)
    }

    @Test
    fun `claim task as not task actor should allow for system`() {
        val testHarryUser = "harry_claim"


        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_not_harry_group")

        AuthContext.runAsFull(testHarryUser, listOf(AuthRole.SYSTEM)) {
            val recordAtt = RecordAtts(EntityRef.create("eproc", ProcTaskRecords.ID, task.id))
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

        val taskAssignee = taskService.createTaskQuery().taskId(task.id).singleResult().assignee

        assertThat(taskAssignee).isEqualTo(testHarryUser)
    }

    @Test
    fun `unclaim task via records`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_test")
        taskService.setAssignee(task.id, "harry")

        AuthContext.runAs("harry") {
            val recordAtt = RecordAtts(EntityRef.create("eproc", ProcTaskRecords.ID, task.id))
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

        val taskAssignee = taskService.createTaskQuery().taskId(task.id).singleResult().assignee

        assertThat(taskAssignee).isNull()
    }

    @Test
    fun `reassign task via records`() {
        val task = taskService.newTask()
        taskService.saveTask(task)

        taskService.addCandidateGroup(task.id, "GROUP_test")
        taskService.setAssignee(task.id, "harry")

        AuthContext.runAs("harry") {
            val recordAtt = RecordAtts(EntityRef.create("eproc", ProcTaskRecords.ID, task.id))
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

        val taskAssignee = taskService.createTaskQuery().taskId(task.id).singleResult().assignee

        assertThat(taskAssignee).isEqualTo("ron")
    }

}
