package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RuntimeService
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
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnVariableInstanceRecords
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcessService
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

        private const val IVAN_PROCESS_ID = "test-bpmn-process-perms-user-ivan"
        private const val KATYA_PROCESS_ID = "test-bpmn-process-perms-user-katya"
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
                                              "fn": "if (value.id.indexOf('ivan') !== -1) {return 'userIvan';} if (value.id.indexOf('katya') !== -1) { return 'userKatya';} return 'userUnknown';"
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
            saveBpmnWithAction(
                "test/bpmn/$procId.bpmn.xml",
                procId,
                BpmnProcessDefActions.DEPLOY
            )

            return bpmnProcessService.startProcess(
                procId,
                procId,
                mapOf(
                    "user" to user,
                    "process" to procId,
                    TEST_ATT_STR to "testStrValue"
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

    @Test
    fun `allow query process instance only with bpmn def engine`() {
        assertThrows<IllegalStateException> {
            queryProcessInstances(EntityRef.EMPTY, "someUser")
        }
    }

    @Test
    fun `user should see only process instance with his permissions`() {
        val ivanProcDefEngine = queryLatestProcessDefEngineRecords(USER_IVAN)[0]
        val ivanProcessInstances = queryProcessInstances(ivanProcDefEngine, USER_IVAN)
        assertThat(ivanProcessInstances).hasSize(1)
        val ivanProcessKey = recordsService.getAtt(ivanProcessInstances[0], "key").asText()
        assertThat(ivanProcessKey).isEqualTo(IVAN_PROCESS_ID)

        val katyaProcDefEngine = queryLatestProcessDefEngineRecords(USER_KATYA)[0]
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
        val procDefsEngine = queryLatestProcessDefEngineRecords("system")
        val allProcesses = procDefsEngine.flatMap { procDefEngineRef ->
            queryProcessInstances(procDefEngineRef, "system")
        }

        assertThat(allProcesses).hasSize(2)
    }

    @Test
    fun `user should see attributes of process instances only with his permissions`() {
        val allProcDefs = queryLatestProcessDefEngineRecords("system")
        val allProcesses = allProcDefs.flatMap { procDefEngineRef ->
            queryProcessInstances(procDefEngineRef, "system")
        }

        val requestedKeysByIvan = AuthContext.runAs(USER_IVAN) {
            getKeysFromProcInstances(allProcesses)
        }
        assertThat(requestedKeysByIvan).hasSize(2)
        val ivanNotEmptyKeys = requestedKeysByIvan.filter {
            it.isNotBlank()
        }
        assertThat(ivanNotEmptyKeys).hasSize(1)
        assertThat(ivanNotEmptyKeys).allMatch {
            it.contains("ivan")
        }

        val requestedKeysByKatya = AuthContext.runAs(USER_KATYA) {
            getKeysFromProcInstances(allProcesses)
        }
        assertThat(requestedKeysByKatya).hasSize(2)
        val katyaNotEmptyKeys = requestedKeysByKatya.filter {
            it.isNotBlank()
        }
        assertThat(katyaNotEmptyKeys).hasSize(1)
        assertThat(katyaNotEmptyKeys).allMatch {
            it.contains("katya")
        }

        val requestedKeysByWithoutPerms = AuthContext.runAs(USER_WITHOUT_PERMS) {
            getKeysFromProcInstances(allProcesses)
        }
        assertThat(requestedKeysByWithoutPerms).hasSize(2)
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
                          "activityId": "Activity_user_task"
                        },
                        {
                          "type": "startBeforeActivity",
                          "activityId": "Event_end_process"
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
                          "activityId": "Activity_user_task"
                        },
                        {
                          "type": "startBeforeActivity",
                          "activityId": "Event_end_process"
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

    @Test
    fun `query variables instances without process instance id should throw exception`() {
        assertThrows<IllegalStateException> {
            queryBpmnVariableInstances("system", "")
        }
    }

    @Test
    fun `user should see variable instances only with his permissions`() {
        val ivanRequestHisVariables = queryBpmnVariableInstances(USER_IVAN, ivanProcessInstanceRef.getLocalId())
        val ivanRequestKatyaVariables = queryBpmnVariableInstances(USER_IVAN, katyaProcessInstanceRef.getLocalId())

        assertThat(ivanRequestHisVariables).isNotEmpty
        assertThat(ivanRequestKatyaVariables).isEmpty()

        val katyaRequestHerVariables = queryBpmnVariableInstances(USER_KATYA, katyaProcessInstanceRef.getLocalId())
        val katyaRequestIvanVariables = queryBpmnVariableInstances(USER_KATYA, ivanProcessInstanceRef.getLocalId())

        assertThat(katyaRequestHerVariables).isNotEmpty
        assertThat(katyaRequestIvanVariables).isEmpty()
    }

    @Test
    fun `user with without read process instance permission should not see variable instances`() {
        val ivanVariables = queryBpmnVariableInstances(USER_WITHOUT_PERMS, ivanProcessInstanceRef.getLocalId())
        val katyaVariables = queryBpmnVariableInstances(USER_WITHOUT_PERMS, katyaProcessInstanceRef.getLocalId())

        assertThat(ivanVariables).isEmpty()
        assertThat(katyaVariables).isEmpty()
    }

    @Test
    fun `system user should see all variable instances`() {
        val ivanVariables = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
        val katyaVariables = queryBpmnVariableInstances("system", katyaProcessInstanceRef.getLocalId())

        assertThat(ivanVariables).isNotEmpty
        assertThat(katyaVariables).isNotEmpty
    }

    @Test
    fun `user should see attributes of variables instances only with his permissions`() {
        val ivanVariables = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
        val katyaVariables = queryBpmnVariableInstances("system", katyaProcessInstanceRef.getLocalId())

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
        val ivanVariables = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
        val katyaVariables = queryBpmnVariableInstances("system", katyaProcessInstanceRef.getLocalId())

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
        val ivanVariable = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId()).first()

        val delStatus = AuthContext.runAs(user) {
            recordsService.delete(ivanVariable)
        }

        assertThat(delStatus).isEqualTo(DelStatus.OK)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_KATYA, USER_WITHOUT_PERMS])
    fun `user without edit instance permission should not allow delete variable instance`(user: String) {
        val ivanVariable = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId()).first()

        val delStatus = AuthContext.runAs(user) {
            recordsService.delete(ivanVariable)
        }

        assertThat(delStatus).isEqualTo(DelStatus.PROTECTED)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_IVAN, "system"])
    fun `user with edit instance permission should allow update variable instance`(user: String) {
        val ivanVariable = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
            .firstNotNullOf {
                val attName = recordsService.getAtts(it, listOf("name"))
                if (attName.getAtt("name").asText() == "testStr") {
                    it
                } else {
                    null
                }
            }

        val newValue = UUID.randomUUID().toString()

        queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId())
            .forEach {
                val value = recordsService.getAtts(it, listOf("type", "value", "name"))
                println("============= value = $value")
            }

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
        val ivanVariable = queryBpmnVariableInstances("system", ivanProcessInstanceRef.getLocalId()).first()

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

    @AfterEach
    fun tearDown() {

        cleanDefinitions()
        cleanDeployments()

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
