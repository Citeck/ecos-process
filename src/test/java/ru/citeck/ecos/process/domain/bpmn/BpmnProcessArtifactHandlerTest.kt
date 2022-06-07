package ru.citeck.ecos.process.domain.bpmn

import org.apache.commons.lang3.LocaleUtils
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.apps.app.service.LocalAppService
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcService
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val BPMN_TEST_PROCESS_ID = "bpmn-test-process"

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnProcessArtifactHandlerTest {

    @Autowired
    private lateinit var localAppService: LocalAppService

    @Autowired
    private lateinit var bpmnProcService: BpmnProcService

    @Autowired
    private lateinit var procDefService: ProcDefService

    @BeforeAll
    fun setUp() {
        localAppService.deployLocalArtifacts()
    }

    @Test
    fun `check definition meta data of loaded process from artifact`() {
        val deployed = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_PROCESS_ID))

        assertNotNull(deployed)

        assertEquals(BPMN_TEST_PROCESS_ID, deployed.id)
        assertEquals(MLText(mapOf(LocaleUtils.toLocale("ru") to "Test process")), deployed.name)

        assertEquals(RecordRef.valueOf("uiserv/form@test-bpmn-form"), deployed.formRef)
        assertEquals(RecordRef.valueOf("emodel/type@type-ecos-fin-request"), deployed.ecosTypeRef)
        assertEquals(BPMN_FORMAT, deployed.format)
        assertEquals(BPMN_PROC_TYPE, deployed.procType)

        assertNull(deployed.alfType)

        assertTrue(deployed.enabled)
        assertTrue(deployed.autoStartEnabled)

        assertTrue(deployed.data != null && deployed.data.isNotEmpty())
    }

    @Test
    fun `loaded process from artifact should be deployed to engine`() {
        val definitions = bpmnProcService.getProcessDefinitionsByKey(BPMN_TEST_PROCESS_ID)

        assertEquals(1, definitions.size)
        assertEquals(BPMN_TEST_PROCESS_ID, definitions.first().key)
        assertEquals(1, definitions.first().version)
    }
}
