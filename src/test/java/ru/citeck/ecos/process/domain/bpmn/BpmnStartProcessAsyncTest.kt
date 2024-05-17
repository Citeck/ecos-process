package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.cleanDefinitions
import ru.citeck.ecos.process.domain.cleanDeployments
import ru.citeck.ecos.process.domain.saveBpmnWithAction
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.concurrent.TimeUnit

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnStartProcessAsyncTest {

    companion object {
        private const val PROC_ID = "bpmn-start-async-test"
        private const val TIMEOUT = 5_000L
    }

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @BeforeAll
    fun setUp() {
        saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )
    }

    @Test
    fun `start bpmn process async test`() {
        val businessKey = "start-bpmn-process-async-test"

        bpmnProcessService.startProcessAsync(
            StartProcessRequest(
                PROC_ID,
                businessKey,
                emptyMap()
            )
        )

        Awaitility.await().atMost(TIMEOUT, TimeUnit.MILLISECONDS).untilAsserted {
            val foundProcess = bpmnProcessService.getProcessInstancesForBusinessKey(businessKey)
            assertThat(foundProcess).hasSize(1)
        }
    }

    @AfterAll
    fun tearDown() {
        cleanDeployments()
        cleanDefinitions()
    }
}
