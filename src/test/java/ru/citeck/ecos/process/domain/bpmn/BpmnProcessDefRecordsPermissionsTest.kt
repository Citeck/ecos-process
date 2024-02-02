package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.EmptyAuth
import ru.citeck.ecos.model.lib.permissions.dto.PermissionLevel
import ru.citeck.ecos.model.lib.permissions.dto.PermissionRule
import ru.citeck.ecos.model.lib.permissions.dto.PermissionsDef
import ru.citeck.ecos.model.lib.role.dto.RoleDef
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.type.dto.TypePermsDef
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelperJava
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmnsection.BpmnSectionPermissionsProvider
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.process.domain.cleanDefinitions
import ru.citeck.ecos.process.domain.cleanDeployments
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.saveAndDeployBpmnFromResource
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.model.perms.registry.TypePermissionsRegistry
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry
import java.util.*
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BpmnProcessDefRecordsPermissionsTest {

    @Autowired
    private lateinit var bpmnProcessDefRecords: BpmnProcessDefRecords

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var permsRegistry: TypePermissionsRegistry

    @Autowired
    private lateinit var typesRegistry: EcosTypesRegistry

    @SpyBean
    private lateinit var bpmnSectionPermissionsProvider: BpmnSectionPermissionsProvider

    private var bpmnTypeBefore: TypeDef? = null
    private var permsDefBefore: TypePermsDef? = null
    private var permsDefId: String = UUID.randomUUID().toString()

    companion object {
        private const val BPMN_PROC_DEF_TYPE_ID = "bpmn-process-def"
        private const val COUNT_OF_PROC_DEF_TO_GENERATE = 250L

        private const val ROLE_WRITE = "roleWrite"
        private const val ROLE_READ = "roleRead"
        private const val ROLE_DEPLOY = "roleDeploy"

        private const val USER_WITH_READ_PERMS = "userRead"
        private const val USER_WITH_WRITE_PERMS = "userWrite"
        private const val USER_WITH_DEPLOY_PERMS = "userDeploy"
        private const val USER_WITHOUT_PERMS = "userWithoutPerms"

        private val queryAllProcDefs = RecordsQuery.create {
            withSourceId(BpmnProcessDefRecords.ID)
            withLanguage(PredicateService.LANGUAGE_PREDICATE)
            withQuery(Predicates.alwaysTrue())
        }
    }

    @BeforeAll
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
                                    .withId(ROLE_WRITE)
                                    .withAssignees(
                                        listOf(USER_WITH_WRITE_PERMS)
                                    )
                                    .build(),
                                RoleDef.create()
                                    .withId(ROLE_READ)
                                    .withAssignees(
                                        listOf(USER_WITH_READ_PERMS)
                                    )
                                    .build(),
                                RoleDef.create {
                                    withId(ROLE_DEPLOY)
                                    withAssignees(
                                        listOf(USER_WITH_DEPLOY_PERMS)
                                    )
                                }
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
                                ROLE_WRITE to mapOf(
                                    "ANY" to PermissionLevel.WRITE
                                ),
                                ROLE_READ to mapOf(
                                    "ANY" to PermissionLevel.READ
                                ),
                                ROLE_DEPLOY to mapOf(
                                    "ANY" to PermissionLevel.WRITE
                                )
                            )
                        )
                        withRules(
                            listOf(
                                PermissionRule(
                                    roles = setOf(ROLE_DEPLOY),
                                    permissions = setOf(
                                        BpmnPermission.PROC_DEF_DEPLOY.id
                                    )
                                ),
                            )
                        )
                    }
                )
            }
        )

        val procDefDtos = (1..COUNT_OF_PROC_DEF_TO_GENERATE).map {
            val id = "def-$it"
            NewProcessDefDto(
                id,
                MLText.EMPTY,
                BPMN_PROC_TYPE,
                "xml",
                "{http://www.citeck.ru/model/test/1.0}test-type",
                ModelUtils.getTypeRef("type1"),
                RecordRef.EMPTY,
                RecordRef.EMPTY,
                BpmnProcHelperJava.buildProcDefXml(id),
                null,
                enabled = true,
                autoStartEnabled = false
            )
        }

        procDefDtos.forEach {
            procDefService.uploadProcDef(it)
        }
    }

    @Test
    @Order(1)
    fun `query without auth should not return defs`() {
        val result = AuthContext.runAs(EmptyAuth) {
            bpmnProcessDefRecords.queryRecords(queryAllProcDefs)
                as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>
        }
        assertEquals(0, result.getRecords().size)
    }

    @Test
    @Order(1)
    fun `query as system should return all defs`() {
        AuthContext.runAsSystem {
            val result = bpmnProcessDefRecords.queryRecords(queryAllProcDefs)
                as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>
            assertEquals(250, result.getRecords().size)
        }
    }

    @Test
    @Order(1)
    fun `query as user with read permissions`() {
        AuthContext.runAs(user = USER_WITH_READ_PERMS) {
            val result = bpmnProcessDefRecords.queryRecords(queryAllProcDefs)
                as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>
            assertEquals(250, result.getRecords().size)
        }
    }

    @Test
    @Order(1)
    fun querySkip100Max50() {
        val querySkip100Max50 = queryAllProcDefs.copy()
            .withSkipCount(100)
            .withMaxItems(50)
            .build()

        val result = bpmnProcessDefRecords.queryRecords(querySkip100Max50)
            as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>

        assertEquals(50, result.getRecords().size)
        assertEquals(true, result.getHasMore())
        assertEquals("def-150", result.getRecords()[0].getId())
        assertEquals(250, result.getTotalCount())
    }

    @Test
    @Order(1)
    fun querySkip20Max30() {
        val querySkip20Max30 = queryAllProcDefs.copy()
            .withSkipCount(20)
            .withMaxItems(30)
            .build()

        val result = bpmnProcessDefRecords.queryRecords(querySkip20Max30)
            as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>

        assertEquals(30, result.getRecords().size)
        assertEquals(true, result.getHasMore())
        assertEquals("def-230", result.getRecords()[0].getId())
        assertEquals(250, result.getTotalCount())
    }

    @Test
    @Order(1)
    fun querySkip120Max30() {
        val querySkip120Max30 = queryAllProcDefs.copy()
            .withSkipCount(120)
            .withMaxItems(30)
            .build()

        val result = bpmnProcessDefRecords.queryRecords(querySkip120Max30)
            as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>

        assertEquals(30, result.getRecords().size)
        assertEquals(true, result.getHasMore())
        assertEquals("def-130", result.getRecords()[0].getId())
        assertEquals(250, result.getTotalCount())
    }

    @Test
    @Order(1)
    fun querySkip249Max101() {
        val querySkip249Max101 = queryAllProcDefs.copy()
            .withSkipCount(249)
            .withMaxItems(101)
            .build()

        val result = bpmnProcessDefRecords.queryRecords(querySkip249Max101)
            as RecsQueryRes<BpmnProcessDefRecords.BpmnProcessDefRecord>

        assertEquals(1, result.getRecords().size)
        assertEquals(false, result.getHasMore())
        assertEquals("def-1", result.getRecords()[0].getId())
        assertEquals(250, result.getTotalCount())
    }

    @Test
    fun `get att as system should allow`() {
        AuthContext.runAsSystem {
            val def = recordsService.getAtt(
                EntityRef.create(BpmnProcessDefRecords.ID, "def-1"),
                "definition"
            ).asText()

            assertThat(def).startsWith("<?xml version")
        }
    }

    @Test
    fun `get att without auth should deny`() {
        AuthContext.runAs(EmptyAuth) {
            val def = recordsService.getAtt(
                EntityRef.create(BpmnProcessDefRecords.ID, "def-1"),
                "definition"
            ).asText()

            assertThat(def).isEmpty()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_WITH_READ_PERMS, USER_WITH_WRITE_PERMS])
    fun `get att with read perms user should allow`(user: String) {
        AuthContext.runAs(user = USER_WITH_READ_PERMS) {
            val def = recordsService.getAtt(
                EntityRef.create(BpmnProcessDefRecords.ID, "def-1"),
                "definition"
            ).asText()

            assertThat(def).startsWith("<?xml version")
        }
    }

    @Test
    fun `get att as user without read perms should deny`() {
        AuthContext.runAs(user = USER_WITHOUT_PERMS) {
            val def = recordsService.getAtt(
                EntityRef.create(BpmnProcessDefRecords.ID, "def-1"),
                "definition"
            ).asText()

            assertThat(def).isEmpty()
        }
    }

    @Test
    fun `delete as empty auth should deny`() {
        AuthContext.runAs(EmptyAuth) {
            val result = bpmnProcessDefRecords.delete("def-3")
            assertEquals(DelStatus.PROTECTED, result)
        }
    }

    @Test
    fun `delete as user without permissions should deny`() {
        val result = AuthContext.runAs(user = USER_WITHOUT_PERMS) {
            bpmnProcessDefRecords.delete("def-2")
        }
        assertEquals(DelStatus.PROTECTED, result)
    }

    @Test
    fun `delete as user with read should deny`() {
        val result = AuthContext.runAs(user = USER_WITH_READ_PERMS) {
            bpmnProcessDefRecords.delete("def-4")
        }
        assertEquals(DelStatus.PROTECTED, result)
    }

    @Test
    @Order(Order.DEFAULT + 1000)
    fun `delete as user with write perms should allow`() {
        AuthContext.runAs(user = USER_WITH_WRITE_PERMS) {
            val result = bpmnProcessDefRecords.delete("def-250")
            assertEquals(DelStatus.OK, result)
        }
    }

    @Test
    fun mutateWithoutPermissions() {
        AuthContext.runAs(EmptyAuth) {

            val recToMutate = bpmnProcessDefRecords.getRecToMutate("def-250")
            recToMutate.enabled = false

            assertThrows<RuntimeException>("Permissions denied. RecordRef: eproc/bpmn-def@def-250") {
                bpmnProcessDefRecords.saveMutatedRec(recToMutate)
            }
        }
    }

    @Test
    fun `mutate as user without permission should deny`() {
        val recToMutate = bpmnProcessDefRecords.getRecToMutate("def-250")
        recToMutate.enabled = false

        assertThrows<RuntimeException>("Permissions denied. RecordRef: eproc/bpmn-def@def-250") {
            AuthContext.runAs(user = "fet") {
                bpmnProcessDefRecords.saveMutatedRec(recToMutate)
            }
        }
    }

    @Test
    fun `mutate as user with write perms should allow`() {
        val recToMutate = bpmnProcessDefRecords.getRecToMutate("def-250")
        recToMutate.enabled = false

        val result = AuthContext.runAs(user = USER_WITH_WRITE_PERMS) {
            bpmnProcessDefRecords.saveMutatedRec(recToMutate)
        }
        assertEquals("def-250", result)
    }

    @Test
    @Order(Order.DEFAULT + 1000)
    fun `deploy as system should allow`() {
        val procId = "definition-test-bpmn-process"

        AuthContext.runAsSystem {
            saveAndDeployBpmnFromResource(
                "test/bpmn/$procId.bpmn.xml",
                procId
            )
        }

        val procDef = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(procDef!!.id).isEqualTo(procId)
    }

    @ParameterizedTest
    @ValueSource(strings = [USER_WITH_READ_PERMS, USER_WITH_WRITE_PERMS, USER_WITHOUT_PERMS])
    fun `deploy as with without deploy permissions should deny`(user: String) {
        val procId = "definition-test-bpmn-process"

        assertThrows<IllegalStateException> {
            AuthContext.runAs(user = user) {
                saveAndDeployBpmnFromResource(
                    "test/bpmn/$procId.bpmn.xml",
                    procId
                )
            }
        }
    }

    @Test
    @Order(Order.DEFAULT + 1000)
    fun `deploy as user with deploy perms should allow`() {
        `when`(
            bpmnSectionPermissionsProvider.hasPermissions(
                RecordRef.valueOf("eproc/bpmn-section@DEFAULT"),
                BpmnPermission.SECTION_CREATE_PROC_DEF
            )
        ).thenReturn(true)

        val procId = "definition-test-bpmn-process"

        AuthContext.runAs(user = USER_WITH_DEPLOY_PERMS) {
            saveAndDeployBpmnFromResource(
                "test/bpmn/$procId.bpmn.xml",
                procId
            )
        }

        val procDef = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(procDef!!.id).isEqualTo(procId)
    }

    @AfterAll
    fun afterAll() {

        cleanDeployments()
        cleanDefinitions()

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
