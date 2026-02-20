package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.permissions.dto.PermissionLevel
import ru.citeck.ecos.model.lib.permissions.dto.PermissionRule
import ru.citeck.ecos.model.lib.permissions.dto.PermissionsDef
import ru.citeck.ecos.model.lib.role.dto.ComputedRoleType
import ru.citeck.ecos.model.lib.role.dto.RoleComputedDef
import ru.citeck.ecos.model.lib.role.dto.RoleDef
import ru.citeck.ecos.model.lib.type.dto.TypePermsDef
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.perms.registry.TypePermissionsRegistry
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.*

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnProcessDefEngineRecordsPermissionsTest {

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var permsRegistry: TypePermissionsRegistry

    @Autowired
    private lateinit var typesRegistry: EcosTypesRegistry

    @Autowired
    private lateinit var helper: BpmnProcHelper

    private lateinit var bpmnTypeBefore: TypeDef
    private var permsDefBefore: TypePermsDef? = null
    private var permsDefId: String = UUID.randomUUID().toString()

    companion object {
        private const val BPMN_PROC_DEF_TYPE_ID = "bpmn-process-def"

        private const val ROLE_READ = "roleRead"

        private const val USER_IVAN = "userIvan"
        private const val USER_KATYA = "userKatya"
        private const val USER_WITHOUT_PERMS = "userWithoutPerms"
    }

    @BeforeEach
    fun setUp() {
        bpmnTypeBefore = typesRegistry.getValue(BPMN_PROC_DEF_TYPE_ID)!!
        permsDefBefore = permsRegistry.getPermissionsForType(ModelUtils.getTypeRef(BPMN_PROC_DEF_TYPE_ID))
        permsDefBefore?.let { permsDefId = it.id }

        typesRegistry.setValue(
            BPMN_PROC_DEF_TYPE_ID,
            bpmnTypeBefore.copy()
                .withModel(
                    bpmnTypeBefore.model.copy()
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
                ).build()
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
                                        BpmnPermission.PROC_INSTANCE_READ.id
                                    )
                                ),
                            )
                        )
                    }
                )
            }
        )

        fun deployDifferentBpmn10Times(procId: String) {
            for (i in 1..10) {
                val procIdIter = "$procId-$i"
                helper.saveBpmnWithActionAndReplaceDefinition(
                    "test/bpmn/$procId.bpmn.xml",
                    procIdIter,
                    BpmnProcessDefActions.DEPLOY,
                    procId to procIdIter
                )
            }
        }

        deployDifferentBpmn10Times("test-bpmn-process-def-engine-perms-user-ivan")
        deployDifferentBpmn10Times("test-bpmn-process-def-engine-perms-user-katya")
    }

    @Test
    fun `user should see only process defs with his permissions`() {
        val userIvanProcDefs = helper.queryLatestProcessDefEngineRecords(USER_IVAN)
        val userKatyaProcDefs = helper.queryLatestProcessDefEngineRecords(USER_KATYA)
        val userWithoutPermsProcDefs = helper.queryLatestProcessDefEngineRecords(USER_WITHOUT_PERMS)

        assertThat(userIvanProcDefs).hasSize(10)
        assertThat(userIvanProcDefs).allMatch {
            val procDefKey = recordsService.getAtt(it, "key").asText()
            procDefKey.contains("ivan")
        }

        assertThat(userKatyaProcDefs).hasSize(10)
        assertThat(userKatyaProcDefs).allMatch {
            val procDefKey = recordsService.getAtt(it, "key").asText()
            procDefKey.contains("katya")
        }

        assertThat(userWithoutPermsProcDefs).hasSize(0)
    }

    @Test
    fun `user should see attributes of process defs only with his permissions`() {
        val allProcDefs = helper.queryLatestProcessDefEngineRecords("system")

        assertThat(allProcDefs).hasSize(20)

        val requestedKeysByIvan = AuthContext.runAs(USER_IVAN) {
            getKeysFromProcDefs(allProcDefs)
        }
        val requestedKeysByKatya = AuthContext.runAs(USER_KATYA) {
            getKeysFromProcDefs(allProcDefs)
        }
        val requestedKeysByWithoutPerms = AuthContext.runAs(USER_WITHOUT_PERMS) {
            getKeysFromProcDefs(allProcDefs)
        }

        assertThat(requestedKeysByIvan).hasSize(20)
        val ivanNotEmptyKeys = requestedKeysByIvan.filter {
            it.isNotBlank()
        }
        assertThat(ivanNotEmptyKeys).hasSize(10)
        assertThat(ivanNotEmptyKeys).allMatch {
            it.contains("ivan")
        }

        assertThat(requestedKeysByKatya).hasSize(20)
        val katyaNotEmptyKeys = requestedKeysByKatya.filter {
            it.isNotBlank()
        }
        assertThat(katyaNotEmptyKeys).hasSize(10)
        assertThat(katyaNotEmptyKeys).allMatch {
            it.contains("katya")
        }

        assertThat(requestedKeysByWithoutPerms).hasSize(20)
        assertThat(requestedKeysByWithoutPerms).allMatch {
            it.isBlank()
        }
    }

    @Test
    fun `as system should see all process defs records`() {
        val allProcDefs = helper.queryLatestProcessDefEngineRecords("system")

        assertThat(allProcDefs).hasSize(20)
    }

    @Test
    fun `as system should see all process defs atts records`() {
        val allProcDefs = helper.queryLatestProcessDefEngineRecords("system")

        assertThat(allProcDefs).hasSize(20)

        val requestedKeysBySystem = AuthContext.runAs("system") {
            getKeysFromProcDefs(allProcDefs)
        }

        assertThat(requestedKeysBySystem).hasSize(20)
        assertThat(requestedKeysBySystem).allMatch {
            it.contains("ivan") || it.contains("katya")
        }
    }

    private fun getKeysFromProcDefs(procDefs: List<EntityRef>): List<String> {
        return procDefs.map {
            recordsService.getAtt(it, "key").asText()
        }
    }

    @AfterEach
    fun tearDown() {

        helper.cleanDeployments()
        helper.cleanDefinitions()

        typesRegistry.setValue(BPMN_PROC_DEF_TYPE_ID, bpmnTypeBefore)

        val permsDefBefore = permsDefBefore
        if (permsDefBefore == null) {
            permsRegistry.setValue(permsDefId, null)
        } else {
            permsRegistry.setValue(permsDefId, permsDefBefore)
        }
    }
}
