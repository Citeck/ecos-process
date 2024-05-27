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
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao.Companion.BPMN_ELEMENTS_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_ELEMENT_DEF_ID
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.process.StartProcessRequest
import ru.citeck.ecos.process.domain.cleanDefinitions
import ru.citeck.ecos.process.domain.cleanDeployments
import ru.citeck.ecos.process.domain.saveBpmnWithAction
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.util.concurrent.TimeUnit

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnStartProcessAsyncTest {

    companion object {
        private const val PROC_ID_START_ASYNC = "bpmn-start-async-test"
        private const val PROC_ID_START_ASYNC_WITH_ERROR = "start-bpmn-process-async-with-error-test"
        private const val TIMEOUT = 5_000L
    }

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    @BeforeAll
    fun setUp() {
        saveBpmnWithAction(
            "test/bpmn/$PROC_ID_START_ASYNC.bpmn.xml",
            PROC_ID_START_ASYNC,
            BpmnProcessDefActions.DEPLOY
        )

        saveBpmnWithAction(
            "test/bpmn/$PROC_ID_START_ASYNC_WITH_ERROR.bpmn.xml",
            PROC_ID_START_ASYNC_WITH_ERROR,
            BpmnProcessDefActions.DEPLOY
        )
    }

    @Test
    fun `start bpmn process async test`() {
        val businessKey = "start-bpmn-process-async-test"

        bpmnProcessService.startProcessAsync(
            StartProcessRequest(
                PROC_ID_START_ASYNC,
                businessKey,
                emptyMap()
            )
        )

        Awaitility.await().atMost(TIMEOUT, TimeUnit.MILLISECONDS).untilAsserted {
            val foundProcess = bpmnProcessService.getProcessInstancesForBusinessKey(businessKey)
            assertThat(foundProcess).hasSize(1)
        }
    }

    @Test
    fun `start bpmn process async with error should not create bpmn elements`() {
        bpmnProcessService.startProcessAsync(
            StartProcessRequest(
                PROC_ID_START_ASYNC_WITH_ERROR,
                null,
                emptyMap()
            )
        )

        TimeUnit.SECONDS.sleep(5)

        val foundBpmnElements = recordsService.query(
            RecordsQuery.create {
                withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                withQuery(
                    Predicates.and(
                        Predicates.eq(BPMN_ELEMENT_DEF_ID, "StartEvent_async_bpmn_error")
                    )
                )
            }
        ).getRecords()

        assertThat(foundBpmnElements).isEmpty()
    }

    @AfterAll
    fun tearDown() {
        cleanDeployments()
        cleanDefinitions()
    }
}
