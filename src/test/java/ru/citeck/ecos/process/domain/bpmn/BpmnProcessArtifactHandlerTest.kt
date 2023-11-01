package ru.citeck.ecos.process.domain.bpmn

import org.apache.commons.lang3.LocaleUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.apps.app.service.LocalAppService
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcessService
import ru.citeck.ecos.process.domain.cleanDefinitions
import ru.citeck.ecos.process.domain.cleanDeployments
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val BPMN_TEST_PROCESS_ID = "bpmn-test-process"
private const val BPMN_TEST_DRAFT_PROCESS_ID = "test-draft-process-artifact-handler"

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnProcessArtifactHandlerTest {

    @Autowired
    private lateinit var localAppService: LocalAppService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var procDefService: ProcDefService

    @BeforeAll
    fun setUp() {
        AuthContext.runAsSystem {
            localAppService.deployLocalArtifacts()
        }
    }

    @AfterAll
    fun tearDown() {
        cleanDefinitions()
        cleanDeployments()
    }

    @Test
    fun `check definition meta data of loaded process from artifact`() {
        val definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_PROCESS_ID))

        assertNotNull(definition)

        assertEquals(BPMN_TEST_PROCESS_ID, definition.id)
        assertEquals(MLText(mapOf(LocaleUtils.toLocale("ru") to "Test process")), definition.name)

        assertEquals(RecordRef.valueOf("uiserv/form@test-bpmn-form"), definition.formRef)
        assertEquals(RecordRef.valueOf("emodel/type@type-ecos-fin-request"), definition.ecosTypeRef)
        assertEquals(BPMN_FORMAT, definition.format)
        assertEquals(BPMN_PROC_TYPE, definition.procType)

        assertNull(definition.alfType)

        assertTrue(definition.enabled)
        assertTrue(definition.autoStartEnabled)

        assertTrue(definition.data != null && definition.data.isNotEmpty())
    }

    @Test
    fun `loaded process should save revision with converted data state`() {
        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_PROCESS_ID))

        assertEquals(1, revisions.size)

        val revision = revisions.first()

        assertEquals(ProcDefRevDataState.CONVERTED, revision.dataState)
    }

    @Test
    fun `loaded process from artifact should be deployed to engine`() {
        val definitions = bpmnProcessService.getProcessDefinitionsByKey(BPMN_TEST_PROCESS_ID)

        assertEquals(1, definitions.size)
        assertEquals(BPMN_TEST_PROCESS_ID, definitions.first().key)
        assertEquals(1, definitions.first().version)
    }

    @Test
    fun `check definition meta data of loaded draft process from artifact`() {
        val definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_DRAFT_PROCESS_ID))

        assertNotNull(definition)

        assertEquals(BPMN_TEST_DRAFT_PROCESS_ID, definition.id)
        assertEquals(MLText(mapOf(LocaleUtils.toLocale("ru") to "Test draft process")), definition.name)

        assertEquals(RecordRef.valueOf("uiserv/form@ecos-test-form"), definition.formRef)
        assertEquals(RecordRef.valueOf("emodel/type@test-type"), definition.ecosTypeRef)

        assertEquals(BPMN_FORMAT, definition.format)
        assertEquals(BPMN_PROC_TYPE, definition.procType)

        assertNull(definition.alfType)

        assertTrue(definition.enabled)

        assertTrue(definition.data != null && definition.data.isNotEmpty())
    }

    @Test
    fun `loaded draft process should save revision with raw data state`() {
        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_DRAFT_PROCESS_ID))

        assertEquals(1, revisions.size)

        val revision = revisions.first()

        assertEquals(ProcDefRevDataState.RAW, revision.dataState)
    }
}
