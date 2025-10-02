package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.BpmnProcHelper
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnStartDifferentProcessInOneDefinitionTest {

    @Autowired
    private lateinit var helper: BpmnProcHelper

    companion object {
        private const val PROC_ID = "start-different-process-from-one-definition-test"
    }

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @BeforeAll
    fun setUp() {
        helper.saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["1", "2"])
    fun `should start different process in one definition`(processIdx: String) {
        bpmnProcessService.startProcess(
            StartProcessRequest(
                "",
                "process_different_$processIdx",
                "test_business_key_$processIdx",
                emptyMap()
            )
        )

        val foundProcess = bpmnProcessService.getProcessInstancesForBusinessKey(
            "test_business_key_$processIdx"
        )

        assertThat(foundProcess).hasSize(1)
    }

    @AfterAll
    fun tearDown() {
        helper.cleanDeployments()
        helper.cleanDefinitions()
    }
}
