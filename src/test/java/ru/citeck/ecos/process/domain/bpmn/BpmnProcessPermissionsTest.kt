package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.rest.dto.migration.MigrationExecutionDto
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.permissions.dto.PermissionLevel
import ru.citeck.ecos.model.lib.permissions.dto.PermissionRule
import ru.citeck.ecos.model.lib.permissions.dto.PermissionsDef
import ru.citeck.ecos.model.lib.role.dto.ComputedRoleType
import ru.citeck.ecos.model.lib.role.dto.RoleComputedDef
import ru.citeck.ecos.model.lib.role.dto.RoleDef
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.type.dto.TypePermsDef
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.*
import ru.citeck.ecos.process.domain.bpmn.api.records.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.QueryPage
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.perms.registry.TypePermissionsRegistry
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.*

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class BpmnProcessPermissionsTest {

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var permsRegistry: TypePermissionsRegistry

    @Autowired
    private lateinit var typesRegistry: EcosTypesRegistry

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var camundaRuntimeService: RuntimeService

    private var bpmnTypeBefore: TypeDef? = null
    private var permsDefBefore: TypePermsDef? = null
    private var permsDefId: String = UUID.randomUUID().toString()

    private lateinit var ivanProcessInstanceRef: EntityRef
    private lateinit var katyaProcessInstanceRef: EntityRef

    companion object {
        private const val BPMN_PROC_DEF_TYPE_ID = "bpmn-process-def"

        private const val ROLE_READ = "roleRead"

        private const val USER_IVAN = "userIvan"
        private const val USER_KATYA = "userKatya"
        private const val USER_WITHOUT_PERMS = "userWithoutPerms"

        private const val TEST_ATT_STR = "testStr"

        private const val IVAN_PROCESS_ID = "test-process-perms-ivan"
        private const val KATYA_PROCESS_ID = "test-process-perms-katya"

        private const val IVAN_JOBS_DEFINITION = "PT5S"
        private const val KATYA_JOBS_DEFINITION = "PT7S"
    }

    @BeforeEach
    fun setUp() {
        bpmnTypeBefore = typesRegistry.getValue(BPMN_PROC_DEF_TYPE_ID)
        permsDefBefore = permsRegistry.getPermissionsForType(ModelUtils.getTypeRef(BPMN_PROC_DEF_TYPE_ID))
        permsDefBefore?.let { permsDefId = it.id }

        typesRegistry.setValue(
            BPMN_PROC_DEF_TYPE_ID,
            TypeDef.create {
                withId(BPMN_PROC_DEF_TYPE_ID)
                withModel(
                    TypeModelDef.create()
                        .withRoles(
                            listOf(
                                RoleDef.create()
                                    .withId(ROLE_READ)
                                    .withComputed(
                                        RoleComputedDef(
                                            type = ComputedRoleType.SCRIPT,
                                            ObjectData.create(
                                                """
                                           {
                                              "fn": "if (value.getId().indexOf('ivan') !== -1) {return 'userIvan';} if (value.getId().indexOf('katya') !== -1) { return 'userKatya';} return 'userUnknown';"
                                            }
                                                """.trimIndent()
                                            )
                                        )
                                    )
                                    .build()
                            )
                        )
                        .build()
                )
            }
        )

        permsRegistry.setValue(
            permsDefId,
            TypePermsDef.create {
                withId(permsDefId)
                withTypeRef(ModelUtils.getTypeRef(BPMN_PROC_DEF_TYPE_ID))
                withPermissions(
                    PermissionsDef.create {
                        withMatrix(
                            mapOf(
                                ROLE_READ to mapOf(
                                    "ANY" to PermissionLevel.READ
                                )
                            )
                        )
                        withRules(
                            listOf(
                                PermissionRule(
                                    roles = setOf(ROLE_READ),
                                    permissions = setOf(
                                        BpmnPermission.PROC_INSTANCE_RUN.id,
                                        BpmnPermission.PROC_INSTANCE_EDIT.id,
                                        BpmnPermission.PROC_INSTANCE_READ.id,
                                        BpmnPermission.PROC_INSTANCE_MIGRATE.id
                                    )
                                )
                            )
                        )
                    }
                )
            }
        )

        fun deployBpmnAndStartProcess(procId: String, user: String): ProcessInstance {
            helper.saveBpmnWithAction(
                "test/bpmn/$procId.bpmn.xml",
                procId,
                BpmnProcessDefActions.DEPLOY
            )

            return bpmnProcessService.startProcess(
                StartProcessRequest(
                    "",
                    procId,
                    procId,
                    mapOf(
                        "user" to user,
                        "process" to procId,
                        TEST_ATT_STR to "testStrValue"
                    )
                )
            )
        }

        val ivanProcessInstanceId = deployBpmnAndStartProcess(IVAN_PROCESS_ID, USER_IVAN).processInstanceId
        val katyaProcessInstanceId = deployBpmnAndStartProcess(KATYA_PROCESS_ID, USER_KATYA).processInstanceId

        ivanProcessInstanceRef = EntityRef.create(
            AppName.EPROC,
            BpmnProcessRecords.ID,
            ivanProcessInstanceId
        )

        katyaProcessInstanceRef = EntityRef.create(
            AppName.EPROC,
            BpmnProcessRecords.ID,
            katyaProcessInstanceId
        )
    }

    // --- PROCESS INSTANCES ---

    @Test
    fun `allow query process instance only with bpmn def engine`() {
        assertThrows<IllegalStateException> {
            queryProcessInstances(EntityRef.EMPTY, "someUser")
        }
    }

    @Test
    fun `user should see only process instance with his permissions`() {
        val ivanProcDefEngine = helper.queryLatestProcessDefEngineRecords(USER_IVAN)[0]
        val ivanProcessInstances = queryProcessInstances(ivanProcDefEngine, USER_IVAN)
        assertThat(ivanProcessInstances).hasSize(1)
        val ivanProcessKey = recordsService.getAtt(ivanProcessInstances[0], "key").asText()
        assertThat(ivanProcessKey).isEqualTo(IVAN_PROCESS_ID)

        val katyaProcDefEngine = helper.queryLatestProcessDefEngineRecords(USER_KATYA)[0]
        val katyaProcessInstances = queryProcessInstances(katyaProcDefEngine, USER_KATYA)
        assertThat(katyaProcessInstances).hasSize(1)
        val katyaProcessKey = recordsService.getAtt(katyaProcessInstances[0], "key").asText()
        assertThat(katyaProcessKey).isEqualTo(KATYA_PROCESS_ID)

        val userWithoutPermsQueryIvanInstances = queryProcessInstances(ivanProcDefEngine, USER_WITHOUT_PERMS)
        val userWithoutPermsQueryKatyaInstances = queryProcessInstances(katyaProcDefEngine, USER_WITHOUT_PERMS)
        assertThat(userWithoutPermsQueryIvanInstances).hasSize(0)
        assertThat(userWithoutPermsQueryKatyaInstances).hasSize(0)

        val katyaGetIvanInstances = queryProcessInstances(ivanProcDefEngine, USER_KATYA)
        assertThat(katyaGetIvanInstances).hasSize(0)

        val ivanGetKatyaInstances = queryProcessInstances(katyaProcDefEngine, USER_IVAN)
        assertThat(ivanGetKatyaInstances).hasSize(0)
    }

    @Test
    fun `system user should see all process instances`() {
        val procDefsEngine = helper.queryLatestProcessDefEngineRecords("system")
        val allProcesses = procDefsEngine.flatMap { procDefEngineRef ->
            queryProcessInstances(procDefEngineRef, "system")
        }

        assertThat(allProcesses).hasSize(6)
    }

    @Test
    fun `user should see attributes of process instances only with his permissions`() {
        val allProcDefs = helper.queryLatestProcessDefEngineRecords("system")
        val allProcesses = allProcDefs.flatMap { procDefEngineRef ->
            queryProcessInstances(procDefEngineRef, "system")
        }

        val requestedKeysByIvan = AuthContext.runAs(USER_IVAN) {
            getKeysFromProcInstances(allProcesses)
        }
        assertThat(requestedKeysByIvan).hasSize(6)
        val ivanNotEmptyKeys = requestedKeysByIvan.filter {
            it.isNotBlank()
        }
        assertThat(ivanNotEmptyKeys).hasSize(3)
        assertThat(ivanNotEmptyKeys).allMatch {
            it.contains("ivan")
        }

        val requestedKeysByKatya = AuthContext.runAs(USER_KATYA) {
            getKeysFromProcInstances(allProcesses)
        }
        assertThat(requestedKeysByKatya).hasSize(6)
        val katyaNotEmptyKeys = requestedKeysByKatya.filter {
            it.isNotBlank()
        }
        assertThat(katyaNotEmptyKeys).hasSize(3)
        assertThat(katyaNotEmptyKeys).allMatch {
            it.contains("katya")
        }

        val requestedKeysByWithoutPerms = AuthContext.runAs(USER_WITHOUT_PERMS) {
            getKeysFromProcInstances(allProcesses)
        }
        assertThat(requestedKeysByWithoutPerms).hasSize(6)
        assertThat(requestedKeysByWithoutPerms).allMatch {
            it.isBlank()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with start instance permission should allow start`(user: String) {
        val document = EntityRef.valueOf("eproc/doc@someDocument")

        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
            this[BPMN_DOCUMENT] = document.toString()
        }

        val startedProcess = AuthContext.runAs(user) {
            recordsService.mutate(startProcessAtts)
        }
        val documentAtt = recordsService.getAtt(startedProcess, "documentRef?id").asText()

        assertThat(documentAtt).isEqualTo(document.toString())
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without start instance permission should not allow start`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(USER_WITHOUT_PERMS) {
                recordsService.mutate(startProcessAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow update variables`(user: String) {
        val mutateAtts = RecordAtts(
            ivanProcessInstanceRef,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.UPDATE.name
            this["foo"] = "bar"
        }

        AuthContext.runAs(user) {
            recordsService.mutate(mutateAtts)
        }

        val fooAtt = camundaRuntimeService.getVariable(ivanProcessInstanceRef.getLocalId(), "foo")

        assertThat(fooAtt).isEqualTo("bar")
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow update variables`(user: String) {
        val mutateAtts = RecordAtts(
            ivanProcessInstanceRef,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.UPDATE.name
            this["foo"] = "bar"
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(mutateAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow delete instance`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        val deleteAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.DELETE.name
        }

        AuthContext.runAs(user) {
            recordsService.mutate(deleteAtts)
        }

        val findDeletedInstance = bpmnProcessService.getProcessInstance(startedProcess.getLocalId())
        assertThat(findDeletedInstance).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow delete instance`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        val deleteAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.DELETE.name
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(deleteAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow suspend process`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        val suspendAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.SUSPEND.name
        }

        AuthContext.runAs(user) {
            recordsService.mutate(suspendAtts)
        }

        val findSuspendedInstance = bpmnProcessService.getProcessInstance(startedProcess.getLocalId())
        assertThat(findSuspendedInstance?.isSuspended).isTrue
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow suspend process`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        val suspendAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.SUSPEND.name
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(suspendAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow activate process`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        bpmnProcessService.suspendProcess(startedProcess.getLocalId())

        val activateAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.ACTIVATE.name
        }

        AuthContext.runAs(user) {
            recordsService.mutate(activateAtts)
        }

        val findActivatedInstance = bpmnProcessService.getProcessInstance(startedProcess.getLocalId())
        assertThat(findActivatedInstance?.isSuspended).isFalse
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow activate process`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        bpmnProcessService.suspendProcess(startedProcess.getLocalId())

        val activateAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.ACTIVATE.name
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(activateAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow modify aka move token`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        val modifyAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.MODIFY.name
            this["data"] = DataValue.create(
                """
                    {
                      "skipCustomListeners": true,
                      "skipIoMappings": true,
                      "instructions": [
                        {
                          "type": "cancel",
                          "activityId": "Activity_called_root_ivan"
                        },
                        {
                          "type": "startBeforeActivity",
                          "activityId": "Event_end"
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        AuthContext.runAs(user) {
            recordsService.mutate(modifyAtts)
        }

        val findCompletedProcess = bpmnProcessService.getProcessInstance(startedProcess.getLocalId())
        assertThat(findCompletedProcess).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permissions should not allow modify aka move token`(user: String) {
        val startProcessAtts = RecordAtts(
            EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, IVAN_PROCESS_ID)
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.START.name
        }
        val startedProcess = recordsService.mutate(startProcessAtts)

        val modifyAtts = RecordAtts(
            startedProcess,
        ).apply {
            this["action"] = BpmnProcessRecords.MutateAction.MODIFY.name
            this["data"] = DataValue.create(
                """
                    {
                      "skipCustomListeners": true,
                      "skipIoMappings": true,
                      "instructions": [
                        {
                          "type": "cancel",
                          "activityId": "Activity_called_root_katya"
                        },
                        {
                          "type": "startBeforeActivity",
                          "activityId": "Event_end"
                        }
                      ]
                    }
                """.trimIndent()
            )
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(modifyAtts)
            }
        }
    }

    // --- BPMN VARIABLE INSTANCE ---
    @Test
    fun `query variables instances without process instance id should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryBpmnVariableInstances("system", "")
        }
    }

    @Test
    fun `user should see variable instances only with his permissions`() {
        val ivanRequestHisVariables = helper.queryBpmnVariableInstances(USER_IVAN, ivanProcessInstanceRef.getLocalId())
        val ivanRequestKatyaVariables = helper.queryBpmnVariableInstances(USER_IVAN, katyaProcessInstanceRef.getLocalId())

        assertThat(ivanRequestHisVariables).isNotEmpty
        assertThat(ivanRequestKatyaVariables).isEmpty()

        val katyaRequestHerVariables = helper.queryBpmnVariableInstances(USER_KATYA, katyaProcessInstanceRef.getLocalId())
        val katyaRequestIvanVariables = helper.queryBpmnVariableInstances(USER_KATYA, ivanProcessInstanceRef.getLocalId())

        assertThat(katyaRequestHerVariables).isNotEmpty
        assertThat(katyaRequestIvanVariables).isEmpty()
    }

    @Test
    fun `user with without read process instance permission should not see variable instances`() {
        val ivanVariables = helper.queryBpmnVariableInstances(USER_WITHOUT_PERMS, ivanProcessInstanceRef.getLocalId())
        val katyaVariables = helper.queryBpmnVariableInstances(USER_WITHOUT_PERMS, katyaProcessInstanceRef.getLocalId())

        assertThat(ivanVariables).isEmpty()
        assertThat(katyaVariables).isEmpty()
    }

    @Test
    fun `system user should see all variable instances`() {
        val ivanVariables = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
        val katyaVariables = helper.queryBpmnVariableInstances("system", katyaProcessInstanceRef.getLocalId())

        assertThat(ivanVariables).isNotEmpty
        assertThat(katyaVariables).isNotEmpty
    }

    @Test
    fun `user should see attributes of variables instances only with his permissions`() {
        val ivanVariables = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
        val katyaVariables = helper.queryBpmnVariableInstances("system", katyaProcessInstanceRef.getLocalId())

        val ivanGetHisAtts = AuthContext.runAs(USER_IVAN) {
            getNamesFromVariableInstances(ivanVariables)
        }
        val ivanGetKatyaAtts = AuthContext.runAs(USER_IVAN) {
            getNamesFromVariableInstances(katyaVariables)
        }
        assertThat(ivanGetHisAtts.filter { it.isNotBlank() }).isNotEmpty
        assertThat(ivanGetKatyaAtts.filter { it.isNotBlank() }).isEmpty()

        val katyaGetHerAtts = AuthContext.runAs(USER_KATYA) {
            getNamesFromVariableInstances(katyaVariables)
        }
        val katyaGetIvanAtts = AuthContext.runAs(USER_KATYA) {
            getNamesFromVariableInstances(ivanVariables)
        }
        assertThat(katyaGetHerAtts.filter { it.isNotBlank() }).isNotEmpty
        assertThat(katyaGetIvanAtts.filter { it.isNotBlank() }).isEmpty()
    }

    @Test
    fun `system user should see all attributes of variables instances`() {
        val ivanVariables = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
        val katyaVariables = helper.queryBpmnVariableInstances("system", katyaProcessInstanceRef.getLocalId())

        val ivanAtts = AuthContext.runAs("system") {
            getNamesFromVariableInstances(ivanVariables)
        }
        val katyaAtts = AuthContext.runAs("system") {
            getNamesFromVariableInstances(katyaVariables)
        }
        assertThat(ivanAtts.filter { it.isNotBlank() }).isNotEmpty
        assertThat(katyaAtts.filter { it.isNotBlank() }).isNotEmpty
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow delete variable instance`(user: String) {
        val ivanVariable = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId()).first()

        val delStatus = AuthContext.runAs(user) {
            recordsService.delete(ivanVariable)
        }

        assertThat(delStatus).isEqualTo(DelStatus.OK)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow delete variable instance`(user: String) {
        val ivanVariable = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId()).first()

        val delStatus = AuthContext.runAs(user) {
            recordsService.delete(ivanVariable)
        }

        assertThat(delStatus).isEqualTo(DelStatus.PROTECTED)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow update variable instance`(user: String) {
        val ivanVariable = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
            .firstNotNullOf {
                val attName = recordsService.getAtts(it, listOf("name"))
                if (attName.getAtt("name").asText() == "testStr") {
                    it
                } else {
                    null
                }
            }

        val newValue = UUID.randomUUID().toString()

        val updateAtts = RecordAtts(
            EntityRef.create(
                AppName.EPROC,
                BpmnVariableInstanceRecords.ID,
                ivanVariable.getLocalId()
            ),
        ).apply {
            this["type"] = "string"
            this["value"] = newValue
        }

        AuthContext.runAs(user) {
            recordsService.mutate(updateAtts)
        }

        val updatedValue = camundaRuntimeService.getVariable(
            ivanProcessInstanceRef.getLocalId(),
            TEST_ATT_STR
        )
        assertThat(updatedValue).isEqualTo(newValue)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow update variable instance`(user: String) {
        val ivanVariable = helper.queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId()).first()

        val newValue = UUID.randomUUID().toString()

        val updateAtts = RecordAtts(
            ivanVariable,
        ).apply {
            this["type"] = "string"
            this["value"] = newValue
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(updateAtts)
            }
        }
    }

    // --- BPMN CALLED PROCESS INSTANCE ---
    @Test
    fun `query called process instances without process instance id should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryCalledProcessInstancesKeys("system", "")
        }
    }

    @Test
    fun `user should see called process instances only with his permissions`() {
        val ivanCalledProcessesKeys = helper.queryCalledProcessInstancesKeys(USER_IVAN, ivanProcessInstanceRef.getLocalId())
        assertThat(ivanCalledProcessesKeys).hasSize(1)
        assertThat(ivanCalledProcessesKeys[0]).isEqualTo("test-process-perms-ivan-1")

        val katyaCalledProcessesKeys = helper.queryCalledProcessInstancesKeys(USER_KATYA, katyaProcessInstanceRef.getLocalId())
        assertThat(katyaCalledProcessesKeys).hasSize(1)
        assertThat(katyaCalledProcessesKeys[0]).isEqualTo("test-process-perms-katya-1")
    }

    @Test
    fun `user without perms should not see called process instances`() {
        val withoutPermsIvanCalledProcessesKeys = helper.queryCalledProcessInstancesKeys(
            USER_WITHOUT_PERMS,
            ivanProcessInstanceRef.getLocalId()
        )
        assertThat(withoutPermsIvanCalledProcessesKeys).isEmpty()

        val withoutPermsKatyaCalledProcessesKeys = helper.queryCalledProcessInstancesKeys(
            USER_WITHOUT_PERMS,
            katyaProcessInstanceRef.getLocalId()
        )
        assertThat(withoutPermsKatyaCalledProcessesKeys).isEmpty()
    }

    @Test
    fun `system user should see all called process instances`() {
        val systemIvanCalledProcessesKeys = helper.queryCalledProcessInstancesKeys(
            "system",
            ivanProcessInstanceRef.getLocalId()
        )
        assertThat(systemIvanCalledProcessesKeys).hasSize(1)
        assertThat(systemIvanCalledProcessesKeys[0]).isEqualTo("test-process-perms-ivan-1")

        val systemKatyaCalledProcessesKeys =
            helper.queryCalledProcessInstancesKeys(
                "system",
                katyaProcessInstanceRef.getLocalId()
            )
        assertThat(systemKatyaCalledProcessesKeys).hasSize(1)
        assertThat(systemKatyaCalledProcessesKeys[0]).isEqualTo("test-process-perms-katya-1")
    }

    // --- BPMN EXTERNAL TASK ---
    @Test
    fun `query external task without process instance id should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryExternalTasks("system", "")
        }
    }

    @Test
    fun `user should see external tasks only with his permissions`() {
        val allIvanExternalTasks = queryAllExternalTaskForUser(USER_IVAN)

        assertThat(allIvanExternalTasks).hasSize(1)
        val ivanExternalTaskActivityId = recordsService.getAtt(allIvanExternalTasks[0], "activityId").asText()
        assertThat(ivanExternalTaskActivityId).isEqualTo("Activity_ivan_external_task")

        val allKatyaExternalTasks = queryAllExternalTaskForUser(USER_KATYA)

        assertThat(allKatyaExternalTasks).hasSize(1)
        val katyaExternalTaskActivityId = recordsService.getAtt(allKatyaExternalTasks[0], "activityId").asText()
        assertThat(katyaExternalTaskActivityId).isEqualTo("Activity_katya_external_task")
    }

    @Test
    fun `user without perms should not see external tasks`() {
        val allWithoutPermsExternalTasks = queryAllExternalTaskForUser(USER_WITHOUT_PERMS)
        assertThat(allWithoutPermsExternalTasks).isEmpty()
    }

    @Test
    fun `system user should see all external tasks`() {
        val allSystemExternalTasks = queryAllExternalTaskForUser("system")
        assertThat(allSystemExternalTasks).hasSize(2)
    }

    @Test
    fun `user should see attributes of external tasks only with his permissions`() {
        val allIvanExternalTasks = queryAllExternalTaskForUser(USER_IVAN)
        val allKatyaExternalTasks = queryAllExternalTaskForUser(USER_KATYA)

        val ivanExternalTaskActivityIds = AuthContext.runAs(USER_IVAN) {
            getActivityIdsFromExternalTasks(allIvanExternalTasks)
        }
        assertThat(ivanExternalTaskActivityIds.filter { it.isNotBlank() }).hasSize(1)

        val katyaExternalTaskActivityIds = AuthContext.runAs(USER_KATYA) {
            getActivityIdsFromExternalTasks(allKatyaExternalTasks)
        }
        assertThat(katyaExternalTaskActivityIds.filter { it.isNotBlank() }).hasSize(1)

        val ivanRequestKatyaExternalTaskActivityIds = AuthContext.runAs(USER_IVAN) {
            getActivityIdsFromExternalTasks(allKatyaExternalTasks)
        }
        assertThat(ivanRequestKatyaExternalTaskActivityIds.filter { it.isNotBlank() }).isEmpty()

        val katyaRequestIvanExternalTaskActivityIds = AuthContext.runAs(USER_KATYA) {
            getActivityIdsFromExternalTasks(allIvanExternalTasks)
        }
        assertThat(katyaRequestIvanExternalTaskActivityIds.filter { it.isNotBlank() }).isEmpty()
    }

    @Test
    fun `user without perms should not see attributes of external tasks`() {
        val allWithoutPermsExternalTasks = queryAllExternalTaskForUser(USER_WITHOUT_PERMS)
        assertThat(allWithoutPermsExternalTasks).isEmpty()
    }

    @Test
    fun `system user should see all attributes of external tasks`() {
        val allSystemExternalTasks = queryAllExternalTaskForUser("system")
        assertThat(allSystemExternalTasks).hasSize(2)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow edit retries external task`(user: String) {
        val ivanExternalTask = queryAllExternalTaskForUser(USER_IVAN).first()

        val updateAtts = RecordAtts(
            ivanExternalTask,
        ).apply {
            this[BpmnExternalTaskRecords.ATT_RETRIES] = 1
        }

        assertDoesNotThrow {
            AuthContext.runAs(user) {
                recordsService.mutate(updateAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow edit retries external task`(user: String) {
        val ivanExternalTask = queryAllExternalTaskForUser(USER_IVAN).first()

        val updateAtts = RecordAtts(
            ivanExternalTask,
        ).apply {
            this[BpmnExternalTaskRecords.ATT_RETRIES] = 1
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(updateAtts)
            }
        }
    }

    // --- BPMN INCIDENT ---
    @Test
    fun `query incidents without process instance should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryIncidentsForProcessInstance("system", "")
        }
    }

    @Test
    fun `query incidents without bpmn def engine should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryIncidentsForProcessInstance("system", "")
        }
    }

    @Test
    fun `user should see incidents only with his permissions query process instance`() {
        val ivanIncidents = helper.queryIncidentsForProcessInstance(USER_IVAN, ivanProcessInstanceRef.toString())
        assertThat(ivanIncidents).isNotEmpty
        val ivanIncidentFailedActivityIds = getFailedActivityIdsFromIncidents(ivanIncidents)
        assertThat(ivanIncidentFailedActivityIds).allMatch {
            it == "Activity_called_root_ivan"
        }

        val katyaIncidents = helper.queryIncidentsForProcessInstance(USER_KATYA, katyaProcessInstanceRef.toString())
        assertThat(katyaIncidents).isNotEmpty
        val katyaIncidentFailedActivityIds = getFailedActivityIdsFromIncidents(katyaIncidents)
        assertThat(katyaIncidentFailedActivityIds).allMatch {
            it == "Activity_called_root_katya"
        }
    }

    @Test
    fun `user should see incidents only with his permissions query bpmn def engine`() {
        val ivanBpmnDefEngine = helper.queryLatestProcessDefEngineRecords(USER_IVAN).first()
        val ivanIncidents = helper.queryIncidentsForBpmnDefEngine(USER_IVAN, ivanBpmnDefEngine.toString())
        assertThat(ivanIncidents).isNotEmpty
        val ivanIncidentFailedActivityIds = getFailedActivityIdsFromIncidents(ivanIncidents)
        assertThat(ivanIncidentFailedActivityIds).allMatch {
            it == "Activity_called_root_ivan"
        }

        val katyaBpmnDefEngine = helper.queryLatestProcessDefEngineRecords(USER_KATYA).first()
        val katyaIncidents = helper.queryIncidentsForBpmnDefEngine(USER_KATYA, katyaBpmnDefEngine.toString())
        assertThat(katyaIncidents).isNotEmpty
        val katyaIncidentFailedActivityIds = getFailedActivityIdsFromIncidents(katyaIncidents)
        assertThat(katyaIncidentFailedActivityIds).allMatch {
            it == "Activity_called_root_katya"
        }
    }

    @Test
    fun `user without perms should not see incidents query for process instance`() {
        val withoutPermsIvanIncidents = helper.queryIncidentsForProcessInstance(
            USER_WITHOUT_PERMS,
            ivanProcessInstanceRef.toString()
        )
        assertThat(withoutPermsIvanIncidents).isEmpty()

        val withoutPermsKatyaIncidents = helper.queryIncidentsForProcessInstance(
            USER_WITHOUT_PERMS,
            katyaProcessInstanceRef.toString()
        )
        assertThat(withoutPermsKatyaIncidents).isEmpty()
    }

    @Test
    fun `user without perms should not see incidents query for bpmn def engine`() {
        val ivanBpmnDefEngine = helper.queryLatestProcessDefEngineRecords(USER_IVAN).first()
        val withoutPermsIvanIncidents = helper.queryIncidentsForBpmnDefEngine(
            USER_WITHOUT_PERMS,
            ivanBpmnDefEngine.toString()
        )
        assertThat(withoutPermsIvanIncidents).isEmpty()

        val katyaBpmnDefEngine = helper.queryLatestProcessDefEngineRecords(USER_KATYA).first()
        val withoutPermsKatyaIncidents = helper.queryIncidentsForBpmnDefEngine(
            USER_WITHOUT_PERMS,
            katyaBpmnDefEngine.toString()
        )
        assertThat(withoutPermsKatyaIncidents).isEmpty()
    }

    @Test
    fun `system user should see all incidents`() {
        val systemIvanIncidents = helper.queryIncidentsForProcessInstance("system", ivanProcessInstanceRef.toString())
        assertThat(systemIvanIncidents).isNotEmpty
        val systemIvanIncidentFailedActivityIds = getFailedActivityIdsFromIncidents(systemIvanIncidents)
        assertThat(systemIvanIncidentFailedActivityIds).allMatch {
            it == "Activity_called_root_ivan"
        }

        val systemKatyaIncidents = helper.queryIncidentsForProcessInstance("system", katyaProcessInstanceRef.toString())
        assertThat(systemKatyaIncidents).isNotEmpty
        val systemKatyaIncidentFailedActivityIds = getFailedActivityIdsFromIncidents(systemKatyaIncidents)
        assertThat(systemKatyaIncidentFailedActivityIds).allMatch {
            it == "Activity_called_root_katya"
        }
    }

    @Test
    fun `user should see attributes of incidents only with his permissions`() {
        val ivanIncidents = helper.queryIncidentsForProcessInstance(USER_IVAN, ivanProcessInstanceRef.toString())
        val ivanIncidentFailedActivityIds = AuthContext.runAs(USER_IVAN) {
            getFailedActivityIdsFromIncidents(ivanIncidents)
        }
        assertThat(ivanIncidentFailedActivityIds.filter { it.isNotBlank() }).allMatch {
            it == "Activity_called_root_ivan"
        }

        val katyaIncidents = helper.queryIncidentsForProcessInstance(USER_KATYA, katyaProcessInstanceRef.toString())
        val katyaIncidentFailedActivityIds = AuthContext.runAs(USER_KATYA) {
            getFailedActivityIdsFromIncidents(katyaIncidents)
        }
        assertThat(katyaIncidentFailedActivityIds.filter { it.isNotBlank() }).allMatch {
            it == "Activity_called_root_katya"
        }

        val ivanRequestKatyaIncidentFailedActivityIds = AuthContext.runAs(USER_IVAN) {
            getFailedActivityIdsFromIncidents(katyaIncidents)
        }
        assertThat(ivanRequestKatyaIncidentFailedActivityIds.filter { it.isNotBlank() }).isEmpty()

        val katyaRequestIvanIncidentFailedActivityIds = AuthContext.runAs(USER_KATYA) {
            getFailedActivityIdsFromIncidents(ivanIncidents)
        }
        assertThat(katyaRequestIvanIncidentFailedActivityIds.filter { it.isNotBlank() }).isEmpty()
    }

    @Test
    fun `system user should see all attributes of incidents`() {
        val ivanIncidents = helper.queryIncidentsForProcessInstance("system", ivanProcessInstanceRef.toString())
        val ivanIncidentFailedActivityIds = AuthContext.runAs("system") {
            getFailedActivityIdsFromIncidents(ivanIncidents)
        }
        assertThat(ivanIncidentFailedActivityIds.filter { it.isNotBlank() }).allMatch {
            it == "Activity_called_root_ivan"
        }

        val katyaIncidents = helper.queryIncidentsForProcessInstance("system", katyaProcessInstanceRef.toString())
        val katyaIncidentFailedActivityIds = AuthContext.runAs("system") {
            getFailedActivityIdsFromIncidents(katyaIncidents)
        }
        assertThat(katyaIncidentFailedActivityIds.filter { it.isNotBlank() }).allMatch {
            it == "Activity_called_root_katya"
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow edit note of incident`(user: String) {
        val ivanIncidents = helper.queryIncidentsForProcessInstance(USER_IVAN, ivanProcessInstanceRef.toString())
        val ivanIncident = ivanIncidents.first()

        val newNote = UUID.randomUUID().toString()

        val updateAtts = RecordAtts(
            ivanIncident,
        ).apply {
            this[BpmnIncidentRecords.ATT_NOTE] = newNote
        }

        AuthContext.runAs(user) {
            recordsService.mutate(updateAtts)
        }

        val updatedNote = recordsService.getAtt(ivanIncident, BpmnIncidentRecords.ATT_NOTE).asText()
        assertThat(updatedNote).isEqualTo(newNote)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow edit note of incident`(user: String) {
        val ivanIncidents = helper.queryIncidentsForProcessInstance(USER_IVAN, ivanProcessInstanceRef.toString())
        val ivanIncident = ivanIncidents.first()

        val updateAtts = RecordAtts(
            ivanIncident,
        ).apply {
            this[BpmnIncidentRecords.ATT_NOTE] = "some note"
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(updateAtts)
            }
        }
    }

    // --- BPMN JOB ---
    @Test
    fun `query bpmn job without process instance should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryBpmnJobs("system", "")
        }
    }

    @Test
    fun `user should see bpmn jobs only with his permissions`() {
        val ivanBpmnJobs = queryAllJobForUser(USER_IVAN)
        assertThat(ivanBpmnJobs).hasSize(2)
        val ivanBpmnJobDefinitions = getActivityIdsForJobs(ivanBpmnJobs)
        assertThat(ivanBpmnJobDefinitions).allMatch {
            it.contains("ivan")
        }

        val katyaBpmnJobs = queryAllJobForUser(USER_KATYA)
        assertThat(katyaBpmnJobs).hasSize(2)
        val katyaBpmnJobDefinitions = getActivityIdsForJobs(katyaBpmnJobs)
        assertThat(katyaBpmnJobDefinitions).allMatch {
            it.contains("katya")
        }
    }

    @Test
    fun `user without perms should not see bpmn jobs`() {
        val withoutPermsIvanBpmnJobs = queryAllJobForUser(USER_WITHOUT_PERMS)
        assertThat(withoutPermsIvanBpmnJobs).isEmpty()
    }

    @Test
    fun `system user should see all bpmn jobs`() {
        val systemIvanBpmnJobs = queryAllJobForUser("system")
        assertThat(systemIvanBpmnJobs).hasSize(4)
    }

    @Test
    fun `user should see attributes of bpmn jobs only with his permissions`() {
        val allJobs = queryAllJobForUser("system")

        val ivanBpmnJobDefinitions = AuthContext.runAs(USER_IVAN) {
            getJobDefinitionsForJobs(allJobs)
        }
        assertThat(ivanBpmnJobDefinitions.filter { it.isNotBlank() }).allMatch {
            it.contains(IVAN_JOBS_DEFINITION)
        }

        val katyaBpmnJobDefinitions = AuthContext.runAs(USER_KATYA) {
            getJobDefinitionsForJobs(allJobs)
        }
        assertThat(katyaBpmnJobDefinitions.filter { it.isNotBlank() }).allMatch {
            it.contains(KATYA_JOBS_DEFINITION)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow suspend job`(user: String) {
        val ivanBpmnJobs = queryAllJobForUser(USER_IVAN)
        val ivanBpmnJob = ivanBpmnJobs.first()

        val updateAtts = RecordAtts(
            ivanBpmnJob,
        ).apply {
            this["action"] = BpmnJobRecords.MutateAction.SUSPEND.toString()
        }

        assertDoesNotThrow {
            AuthContext.runAs(user) {
                recordsService.mutate(updateAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow suspend job`(user: String) {
        val ivanBpmnJobs = queryAllJobForUser(USER_IVAN)
        val ivanBpmnJob = ivanBpmnJobs.first()

        val updateAtts = RecordAtts(
            ivanBpmnJob,
        ).apply {
            this["action"] = BpmnJobRecords.MutateAction.SUSPEND.toString()
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(updateAtts)
            }
        }
    }

    // --- BPMN JOB DEFINITION ---
    @Test
    fun `query bpmn job def without bpmn def engine should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryBpmnJobDefs("system", "")
        }
    }

    @Test
    fun `user should see bpmn job defs only with his permissions`() {
        val ivanBpmnJobs = queryAllJobDefsForUser(USER_IVAN)
        assertThat(ivanBpmnJobs).hasSize(2)
        val ivanBpmnJobDefinitions = getActivityIdsForJobDefs(ivanBpmnJobs)
        assertThat(ivanBpmnJobDefinitions).allMatch {
            it.contains("ivan")
        }

        val katyaBpmnJobs = queryAllJobDefsForUser(USER_KATYA)
        assertThat(katyaBpmnJobs).hasSize(2)
        val katyaBpmnJobDefinitions = getActivityIdsForJobDefs(katyaBpmnJobs)
        assertThat(katyaBpmnJobDefinitions).allMatch {
            it.contains("katya")
        }
    }

    @Test
    fun `user without perms should not see bpmn job defs`() {
        val withoutPermsIvanBpmnJobs = queryAllJobDefsForUser(USER_WITHOUT_PERMS)
        assertThat(withoutPermsIvanBpmnJobs).isEmpty()
    }

    @Test
    fun `system user should see all bpmn job defs`() {
        val systemIvanBpmnJobs = queryAllJobDefsForUser("system")
        assertThat(systemIvanBpmnJobs).hasSize(4)
    }

    @Test
    fun `user should see attributes of bpmn job defs only with his permissions`() {
        val allJobs = queryAllJobDefsForUser("system")

        val ivanBpmnJobDefinitions = AuthContext.runAs(USER_IVAN) {
            getActivityIdsForJobDefs(allJobs)
        }
        assertThat(ivanBpmnJobDefinitions.filter { it.isNotBlank() }).allMatch {
            it.contains("ivan")
        }

        val katyaBpmnJobDefinitions = AuthContext.runAs(USER_KATYA) {
            getActivityIdsForJobDefs(allJobs)
        }
        assertThat(katyaBpmnJobDefinitions.filter { it.isNotBlank() }).allMatch {
            it.contains("katya")
        }
    }

    // --- BPMN MIGRATION ---
    @Test
    fun `query migration plan without source process definition id should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryMigrationPlan("system", "", "someDefId")
        }
    }

    @Test
    fun `query migration plan without target process definition id should throw exception`() {
        assertThrows<IllegalStateException> {
            helper.queryMigrationPlan("system", "someDefId", "")
        }
    }

    @Test
    fun `user should see migration plan only with his permissions`() {
        val ivanProcessDefEngine = helper.queryLatestProcessDefEngineRecords(USER_IVAN)[0]
        val ivanMigrationPlan = helper.queryMigrationPlan(
            USER_IVAN,
            ivanProcessDefEngine.getLocalId(),
            ivanProcessDefEngine.getLocalId()
        )
        assertThat(ivanMigrationPlan).isNotEmpty

        val katyaProcessDefEngine = helper.queryLatestProcessDefEngineRecords(USER_KATYA)[0]
        val katyaMigrationPlan = helper.queryMigrationPlan(
            USER_KATYA,
            katyaProcessDefEngine.getLocalId(),
            katyaProcessDefEngine.getLocalId()
        )
        assertThat(katyaMigrationPlan).isNotEmpty

        val ivanRequestKatyaPlan = helper.queryMigrationPlan(
            USER_IVAN,
            katyaProcessDefEngine.getLocalId(),
            katyaProcessDefEngine.getLocalId()
        )
        assertThat(ivanRequestKatyaPlan).isEmpty()

        val ivanRequestMixedPlan = helper.queryMigrationPlan(
            USER_IVAN,
            katyaProcessDefEngine.getLocalId(),
            ivanProcessDefEngine.getLocalId()
        )
        assertThat(ivanRequestMixedPlan).isEmpty()

        val katyaRequestIvanPlan = helper.queryMigrationPlan(
            USER_KATYA,
            ivanProcessDefEngine.getLocalId(),
            ivanProcessDefEngine.getLocalId()
        )
        assertThat(katyaRequestIvanPlan).isEmpty()

        val katyaRequestMixedPlan = helper.queryMigrationPlan(
            USER_KATYA,
            ivanProcessDefEngine.getLocalId(),
            katyaProcessDefEngine.getLocalId()
        )
        assertThat(katyaRequestMixedPlan).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with migrate permission should allow migrate processes`(user: String) {
        val ivanProcessDefEngine = helper.queryLatestProcessDefEngineRecords(USER_IVAN)[0]
        val processInstances = queryProcessInstances(ivanProcessDefEngine, "system")

        val ivanMigrationPlan = helper.queryMigrationPlan(
            USER_IVAN,
            ivanProcessDefEngine.getLocalId(),
            ivanProcessDefEngine.getLocalId()
        )
        assertThat(ivanMigrationPlan).isNotEmpty

        val migrationAtts = RecordAtts(
            "${BpmnProcessMigrationRecords.ID}@",
        ).apply {
            this["action"] = BpmnProcessMigrationRecords.MutateAction.MIGRATE.toString()
            this["async"] = false
            this["migrationExecution"] = MigrationExecutionDto().apply {
                this.migrationPlan = ivanMigrationPlan.first().migrationPlan
                this.processInstanceIds = processInstances.map { it.getLocalId() }
            }
        }

        assertDoesNotThrow {
            AuthContext.runAs(user) {
                recordsService.mutate(migrationAtts)
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without migrate permission should not allow migrate processes`(user: String) {
        val ivanProcessDefEngine = helper.queryLatestProcessDefEngineRecords(USER_IVAN)[0]
        val processInstances = queryProcessInstances(ivanProcessDefEngine, "system")

        val ivanMigrationPlan = helper.queryMigrationPlan(
            USER_IVAN,
            ivanProcessDefEngine.getLocalId(),
            ivanProcessDefEngine.getLocalId()
        )
        assertThat(ivanMigrationPlan).isNotEmpty

        val migrationAtts = RecordAtts(
            "${BpmnProcessMigrationRecords.ID}@",
        ).apply {
            this["action"] = BpmnProcessMigrationRecords.MutateAction.MIGRATE.toString()
            this["async"] = false
            this["migrationExecution"] = MigrationExecutionDto().apply {
                this.migrationPlan = ivanMigrationPlan.first().migrationPlan
                this.processInstanceIds = processInstances.map { it.getLocalId() }
            }
        }

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user) {
                recordsService.mutate(migrationAtts)
            }
        }
    }

    private fun getKeysFromProcInstances(procInstances: List<EntityRef>): List<String> {
        return procInstances.map {
            recordsService.getAtt(it, "key").asText()
        }
    }

    private fun getNamesFromVariableInstances(variableInstances: List<EntityRef>): List<String> {
        return variableInstances.map {
            recordsService.getAtt(it, "name").asText()
        }
    }

    private fun getActivityIdsFromExternalTasks(externalTasks: List<EntityRef>): List<String> {
        return externalTasks.map {
            recordsService.getAtt(it, "activityId").asText()
        }
    }

    private fun getActivityIdsForJobDefs(jobDefs: List<EntityRef>): List<String> {
        return jobDefs.map {
            recordsService.getAtt(it, BpmnJobDefRecords.ATT_ACTIVITY_ID).asText()
        }
    }

    private fun getFailedActivityIdsFromIncidents(incidents: List<EntityRef>): List<String> {
        return incidents.map {
            recordsService.getAtt(it, "failedActivityId").asText()
        }
    }

    private fun getJobDefinitionsForJobs(jobs: List<EntityRef>): List<String> {
        return jobs.map {
            recordsService.getAtt(
                it,
                "${BpmnJobRecords.ATT_JOB_DEFINITION}.${BpmnJobDefRecords.ATT_CONFIGURATION}"
            ).asText()
        }
    }

    private fun getActivityIdsForJobs(jobs: List<EntityRef>): List<String> {
        return jobs.map {
            recordsService.getAtt(
                it,
                "${BpmnJobRecords.ATT_JOB_DEFINITION}.${BpmnJobDefRecords.ATT_ACTIVITY_ID}"
            ).asText()
        }
    }

    private fun queryProcessInstances(bpmnEngineDef: EntityRef, user: String): List<EntityRef> {
        return AuthContext.runAs(user) {
            recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnProcessRecords.ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            Predicates.eq("bpmnDefEngine", bpmnEngineDef)
                        )
                    )
                    withPage(QueryPage(10_000, 0, null))
                }
            ).getRecords()
        }
    }

    private fun queryAllExternalTaskForUser(user: String): List<EntityRef> {
        return helper.queryLatestProcessDefEngineRecords("system").flatMap { procDefEngine ->
            queryProcessInstances(procDefEngine, "system")
                .flatMap { procInstance ->
                    helper.queryExternalTasks(user, procInstance.toString())
                }
        }
    }

    private fun queryAllJobForUser(user: String): List<EntityRef> {
        return helper.queryLatestProcessDefEngineRecords("system").flatMap { procDefEngine ->
            queryProcessInstances(procDefEngine, "system")
                .flatMap { procInstance ->
                    helper.queryBpmnJobs(user, procInstance.toString())
                }
        }
    }

    private fun queryAllJobDefsForUser(user: String): List<EntityRef> {
        return helper.queryLatestProcessDefEngineRecords("system").flatMap { procDefEngine ->
            helper.queryBpmnJobDefs(user, procDefEngine.toString())
        }
    }

    @AfterEach
    fun tearDown() {

        helper.cleanDeployments()
        helper.cleanDefinitions()

        val bpmnTypeBefore = bpmnTypeBefore
        if (bpmnTypeBefore == null) {
            typesRegistry.setValue(BPMN_PROC_DEF_TYPE_ID, null)
        } else {
            typesRegistry.setValue(BPMN_PROC_DEF_TYPE_ID, bpmnTypeBefore)
        }
        val permsDefBefore = permsDefBefore
        if (permsDefBefore == null) {
            permsRegistry.setValue(permsDefId, null)
        } else {
            permsRegistry.setValue(permsDefId, permsDefBefore)
        }
    }
}
