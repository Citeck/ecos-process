package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessRecords
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPerms
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsApi
import ru.citeck.ecos.webapp.lib.perms.component.custom.CustomRecordPermsData
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnCustomRunPermissionTest {

    @Autowired
    private lateinit var recordsService: RecordsService

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @MockBean
    private lateinit var customPermsApi: CustomRecordPermsApi

    private lateinit var definition: EntityRef
    private lateinit var process: EntityRef

    @BeforeEach
    fun setUp() {
        val id = "custom-run-${UUID.randomUUID()}"
        definition = EntityRef.create(EprocApp.NAME, BpmnProcessDefRecords.ID, id)
        process = EntityRef.create(EprocApp.NAME, BpmnProcessRecords.ID, id)
        `when`(customPermsApi.getPerms(any())).thenAnswer { invocation ->
            if (invocation.arguments[0] == definition) {
                CustomRecordPerms(
                    listOf(
                        CustomRecordPermsData(
                            setOf(BpmnPermission.PROC_INSTANCE_RUN.id),
                            setOf("GROUP_CUSTOM_RUN"),
                            emptySet()
                        )
                    )
                )
            } else {
                CustomRecordPerms.EMPTY
            }
        }
        AuthContext.runAsSystem {
            helper.saveAndDeployBpmnFromString(
                ResourceUtils.getFile("classpath:test/bpmn/simple-vaild-process.bpmn.xml")
                    .readText().replace("simple-vaild-process", id)
                    .replace("ecos:enabled=\"false\"", "ecos:enabled=\"true\""),
                id
            )
        }
    }

    @Test
    fun `custom run grant allows start without granting definition read or instance administration`() {
        AuthContext.runAs("custom-run-user", listOf("GROUP_CUSTOM_RUN")) {
            assertThat(recordsService.getAtt(definition, "definition").asText()).isEmpty()
            assertThat(recordsService.getAtt(definition, BpmnPermission.PROC_INSTANCE_RUN.getAttribute()).asBoolean()).isTrue()
            assertThat(recordsService.getAtt(definition, BpmnPermission.PROC_INSTANCE_EDIT.getAttribute()).asBoolean()).isFalse()
            assertThat(recordsService.getAtt(definition, BpmnPermission.PROC_INSTANCE_MIGRATE.getAttribute()).asBoolean()).isFalse()
            val started = recordsService.mutate(
                RecordAtts(process).apply {
                    this["action"] = "START"
                    this["document"] = "eproc/doc@custom-run-document"
                }
            )
            assertThat(started.getLocalId()).isNotBlank()
        }
    }

    @Test
    fun `user outside custom grant cannot start process`() {
        AuthContext.runAs("other-user", emptyList()) {
            assertThat(recordsService.getAtt(definition, BpmnPermission.PROC_INSTANCE_RUN.getAttribute()).asBoolean()).isFalse()
            assertThrows<IllegalStateException> {
                recordsService.mutate(RecordAtts(process).apply { this["action"] = "START" })
            }
        }
    }

    @AfterEach
    fun tearDown() {
        AuthContext.runAsSystem {
            helper.cleanDeployments()
            helper.cleanDefinitions()
        }
    }
}
