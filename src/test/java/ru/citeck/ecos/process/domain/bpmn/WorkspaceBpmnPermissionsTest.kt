package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.CustomWorkspaceApi
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class WorkspaceBpmnPermissionsTest {

    companion object {
        private const val WORKSPACE = "test-ws"
        private const val PROC_DEF_ID = "ws-perm-test-process"

        private const val MANAGER_USER = "managerUser"
        private const val MEMBER_USER = "memberUser"
        private const val NON_MEMBER_USER = "nonMemberUser"
    }

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var workspaceApi: CustomWorkspaceApi

    @Autowired
    private lateinit var workspaceService: WorkspaceService

    private lateinit var procDefRef: EntityRef

    @BeforeEach
    fun setUp() {
        helper.cleanDeployments()

        workspaceApi.setUserMemberships(
            MANAGER_USER,
            listOf(
                CustomWorkspaceApi.WsMembership(WORKSPACE, isManager = true)
            )
        )
        workspaceApi.setUserWorkspaces(MEMBER_USER, setOf(WORKSPACE))

        val testDef = ResourceUtils.getFile("classpath:test/bpmn/simple-vaild-process.bpmn.xml").readText()
        helper.saveAndDeployBpmnFromString(
            testDef.replace("simple-vaild-process", PROC_DEF_ID),
            PROC_DEF_ID,
            WORKSPACE
        )

        val localId = workspaceService.addWsPrefixToId(PROC_DEF_ID, WORKSPACE)
        procDefRef = EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, localId)
    }

    @AfterEach
    fun tearDown() {
        workspaceApi.cleanUp()
        helper.cleanDefinitions()
        helper.cleanDeployments()
    }

    // --- Diagnostic ---

    @Test
    fun `record should exist and have correct workspace`() {
        AuthContext.runAsSystem {
            val id = recordsService.getAtt(procDefRef, "id").asText()
            val workspace = recordsService.getAtt(procDefRef, "workspace").asText()

            assertThat(id)
                .describedAs("Record should exist. Ref: $procDefRef")
                .isNotBlank()
            assertThat(workspace)
                .describedAs("Record workspace should be $WORKSPACE. Ref: $procDefRef, id: $id")
                .isEqualTo(WORKSPACE)
        }
    }

    // --- Manager permissions ---

    @Test
    fun `manager should have all permissions on local workspace artifact`() {
        AuthContext.runAs(MANAGER_USER, listOf("GROUP_all")) {
            for (perm in BpmnPermission.entries) {
                val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${perm.id}?bool!").asBoolean()
                assertThat(hasPerm)
                    .describedAs("Manager should have permission: ${perm.id}")
                    .isTrue()
            }
        }
    }

    // --- Member permissions ---

    @Test
    fun `member should have read permission on local workspace artifact`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasRead = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.READ.id}?bool!").asBoolean()
            assertThat(hasRead).isTrue()
        }
    }

    @Test
    fun `member should have process instance run permission`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.PROC_INSTANCE_RUN.id}?bool!").asBoolean()
            assertThat(hasPerm).isTrue()
        }
    }

    @Test
    fun `member should have report view permission`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.PROC_DEF_REPORT_VIEW.id}?bool!").asBoolean()
            assertThat(hasPerm).isTrue()
        }
    }

    @Test
    fun `member should have process instance read permission`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.PROC_INSTANCE_READ.id}?bool!").asBoolean()
            assertThat(hasPerm).isTrue()
        }
    }

    @Test
    fun `member should not have write permission on local workspace artifact`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasWrite = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.WRITE.id}?bool!").asBoolean()
            assertThat(hasWrite).isFalse()
        }
    }

    @Test
    fun `member should not have deploy permission`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.PROC_DEF_DEPLOY.id}?bool!").asBoolean()
            assertThat(hasPerm).isFalse()
        }
    }

    @Test
    fun `member should not have process instance edit permission`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.PROC_INSTANCE_EDIT.id}?bool!").asBoolean()
            assertThat(hasPerm).isFalse()
        }
    }

    @Test
    fun `member should not have process instance migrate permission`() {
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${BpmnPermission.PROC_INSTANCE_MIGRATE.id}?bool!").asBoolean()
            assertThat(hasPerm).isFalse()
        }
    }

    @Test
    fun `member should not have section management permissions`() {
        val sectionPerms = listOf(
            BpmnPermission.SECTION_CREATE_PROC_DEF,
            BpmnPermission.SECTION_CREATE_SUBSECTION,
            BpmnPermission.SECTION_EDIT_PROC_DEF
        )
        AuthContext.runAs(MEMBER_USER, listOf("GROUP_all")) {
            for (perm in sectionPerms) {
                val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${perm.id}?bool!").asBoolean()
                assertThat(hasPerm)
                    .describedAs("Member should not have section permission: ${perm.id}")
                    .isFalse()
            }
        }
    }

    // --- Non-member permissions ---

    @Test
    fun `non-member should have no permissions on local workspace artifact`() {
        AuthContext.runAs(NON_MEMBER_USER, listOf("GROUP_all")) {
            for (perm in BpmnPermission.entries) {
                val hasPerm = recordsService.getAtt(procDefRef, "permissions._has.${perm.id}?bool!").asBoolean()
                assertThat(hasPerm)
                    .describedAs("Non-member should not have permission: ${perm.id}")
                    .isFalse()
            }
        }
    }
}
