package ru.citeck.ecos.process.domain.bpmn

import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.data.EmptyAuth
import ru.citeck.ecos.model.lib.permissions.dto.PermissionLevel
import ru.citeck.ecos.model.lib.permissions.dto.PermissionsDef
import ru.citeck.ecos.model.lib.permissions.repo.PermissionsRepo
import ru.citeck.ecos.model.lib.role.dto.RoleDef
import ru.citeck.ecos.model.lib.type.dto.TypeInfo
import ru.citeck.ecos.model.lib.type.dto.TypeModelDef
import ru.citeck.ecos.model.lib.type.dto.TypePermsDef
import ru.citeck.ecos.model.lib.type.repo.TypesRepo
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefRecords
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefRecords.Companion.SOURCE_ID
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.context.model.ModelServiceFactoryConfig
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@Suppress("UNCHECKED_CAST")
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnProcDefRecordsTest {

    @Autowired
    private lateinit var bpmnProcDefRecords: BpmnProcDefRecords

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var modelServiceFactoryConfig: ModelServiceFactoryConfig

    @Autowired
    private lateinit var recordsService: RecordsService

    companion object {
        private const val BPMN_PROC_DEF_TYPE_ID = "bpmn-process-def"
        private const val COUNT_OF_PROC_DEF_TO_GENERATE = 250L
        private val queryAllProcDefs = RecordsQuery.create {
            withSourceId(SOURCE_ID)
            withLanguage(PredicateService.LANGUAGE_PREDICATE)
            withQuery(Predicates.alwaysTrue())
        }
    }

    @BeforeAll
    fun setUp() {
        val typesRepo = object : TypesRepo {
            override fun getTypeInfo(typeRef: EntityRef): TypeInfo? {
                if (typeRef.getLocalId() == BPMN_PROC_DEF_TYPE_ID) {
                    return TypeInfo.create {
                        withId(typeRef.getLocalId())
                        withModel(
                            TypeModelDef.create()
                                .withRoles(
                                    listOf(
                                        RoleDef.create()
                                            .withId("admin")
                                            .withAssignees(
                                                listOf("GROUP_ECOS_ADMINISTRATORS")
                                            )
                                            .build(),
                                        RoleDef.create()
                                            .withId("EVERYONE")
                                            .build()
                                    )
                                )
                                .build()
                        )
                    }
                }
                return null
            }

            override fun getChildren(typeRef: EntityRef): List<RecordRef> {
                return emptyList()
            }
        }

        val permsRepo = object : PermissionsRepo {
            override fun getPermissionsForType(typeRef: EntityRef): TypePermsDef? {
                if (typeRef.getLocalId() == BPMN_PROC_DEF_TYPE_ID) {
                    return TypePermsDef.create {
                        withPermissions(
                            PermissionsDef.create {
                                withMatrix(
                                    mapOf(
                                        "admin" to mapOf(
                                            "ANY" to PermissionLevel.WRITE
                                        ),
                                        "EVERYONE" to mapOf(
                                            "ANY" to PermissionLevel.READ
                                        )
                                    )
                                )
                            }
                        )
                    }
                }
                return null
            }
        }

        modelServiceFactoryConfig.setTypesRepo(typesRepo)
        modelServiceFactoryConfig.setPermsRepo(permsRepo)

        val procDefDtos = (1..COUNT_OF_PROC_DEF_TO_GENERATE).map {
            val id = "def-$it"
            NewProcessDefDto(
                id,
                MLText.EMPTY,
                BPMN_PROC_TYPE,
                "xml",
                "{http://www.citeck.ru/model/test/1.0}test-type",
                TypeUtils.getTypeRef("type1"),
                RecordRef.EMPTY,
                buildProcDefXml(id),
                null,
                true,
                false
            )
        }

        procDefDtos.forEach {
            procDefService.uploadProcDef(it)
        }

        recordsService.register(
            RecordsDaoBuilder.create("alfresco/").build()
        )
    }

    @Test
    fun queryWithoutPermissions() {
        val result = AuthContext.runAs(EmptyAuth) {
            bpmnProcDefRecords.queryRecords(queryAllProcDefs)
                as RecsQueryRes<BpmnProcDefRecords.BpmnProcDefRecord>
        }
        assertEquals(250, result.getRecords().size)
        assertEquals(COUNT_OF_PROC_DEF_TO_GENERATE, result.getTotalCount())
    }

    @Test
    fun queryAsSystem() {
        AuthContext.runAsSystem {
            val result = bpmnProcDefRecords.queryRecords(queryAllProcDefs)
                as RecsQueryRes<BpmnProcDefRecords.BpmnProcDefRecord>
            assertEquals(250, result.getRecords().size)
        }
    }

    @Test
    fun queryAsUser() {
        AuthContext.runAs(user = "fet") {
            val result = bpmnProcDefRecords.queryRecords(queryAllProcDefs)
                as RecsQueryRes<BpmnProcDefRecords.BpmnProcDefRecord>
            assertEquals(250, result.getRecords().size)
        }
    }

    @Test
    fun querySkip100Max50() {
        val querySkip100Max50 = queryAllProcDefs.copy()
            .withSkipCount(100)
            .withMaxItems(50)
            .build()

        val result = AuthContext.runAs(user = "fet") {
            bpmnProcDefRecords.queryRecords(querySkip100Max50)
                as RecsQueryRes<BpmnProcDefRecords.BpmnProcDefRecord>
        }

        assertEquals(50, result.getRecords().size)
        assertEquals("def-150", result.getRecords()[0].getId())
    }

    @Test
    fun deleteWithoutPermissions() {
        AuthContext.runAs(EmptyAuth) {
            val result = bpmnProcDefRecords.delete("def-3")
            assertEquals(DelStatus.PROTECTED, result)
        }
    }

    @Test
    fun deleteAsUser() {
        val result = AuthContext.runAs(user = "fet") {
            bpmnProcDefRecords.delete("def-2")
        }
        assertEquals(DelStatus.PROTECTED, result)
    }

    @AfterAll
    fun deleteAsAdmin() {
        AuthContext.runAs(user = "admin", authorities = listOf("GROUP_ECOS_ADMINISTRATORS")) {
            val result = bpmnProcDefRecords.delete("def-250")
            assertEquals(DelStatus.OK, result)
            assertEquals(COUNT_OF_PROC_DEF_TO_GENERATE - 1, procDefService.getCount())
        }
    }

    @Test
    fun mutateWithoutPermissions() {
        AuthContext.runAs(EmptyAuth) {

            val recToMutate = bpmnProcDefRecords.getRecToMutate("def-250")
            recToMutate.enabled = false

            assertThrows<RuntimeException>("Permissions denied. RecordRef: eproc/bpmn-def@def-250") {
                bpmnProcDefRecords.saveMutatedRec(recToMutate)
            }
        }
    }

    @Test
    fun mutateAsUser() {
        val recToMutate = bpmnProcDefRecords.getRecToMutate("def-250")
        recToMutate.enabled = false

        assertThrows<RuntimeException>("Permissions denied. RecordRef: eproc/bpmn-def@def-250") {
            AuthContext.runAs(user = "fet") {
                bpmnProcDefRecords.saveMutatedRec(recToMutate)
            }
        }
    }

    @Test
    fun mutateAsAdmin() {
        val recToMutate = bpmnProcDefRecords.getRecToMutate("def-250")
        recToMutate.enabled = false

        val result = AuthContext.runAs(user = "admin", authorities = listOf("GROUP_ECOS_ADMINISTRATORS")) {
            bpmnProcDefRecords.saveMutatedRec(recToMutate)
        }
        assertEquals("def-250", result)
    }

    private fun buildProcDefXml(id: String): ByteArray {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                              xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                              xmlns:dc="http://www.omg.org/spec/DD/20100524/DC" xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                              id="Definitions_$id" targetNamespace="http://bpmn.io/schema/bpmn"
                              xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0"
                              exporter="bpmn-js (https://demo.bpmn.io)" exporterVersion="8.2.0"
                              ecos:processDefId="$id">
              <bpmn:process id="Process_$id" isExecutable="false">
                <bpmn:startEvent id="StartEvent_0lly8qf">
                  <bpmn:outgoing>Flow_15brz3r</bpmn:outgoing>
                </bpmn:startEvent>
                <bpmn:sequenceFlow id="Flow_15brz3r" sourceRef="StartEvent_0lly8qf" targetRef="Event_0fitnzy"/>
                <bpmn:endEvent id="Event_0fitnzy">
                  <bpmn:incoming>Flow_15brz3r</bpmn:incoming>
                </bpmn:endEvent>
              </bpmn:process>
            </bpmn:definitions>
        """.trimIndent().toByteArray(StandardCharsets.UTF_8)
    }
}
