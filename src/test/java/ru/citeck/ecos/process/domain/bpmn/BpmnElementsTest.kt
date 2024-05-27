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
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import java.util.concurrent.TimeUnit

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class], properties = ["ecos-process.bpmn.elements.listener.enabled=true"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnElementsTest {

    companion object {
        private const val PROC_ID = "bpmn-elements-multiple-time-in-one-flow"
    }

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    @BeforeAll
    fun setUp() {
        saveBpmnWithAction(
            "test/bpmn/$PROC_ID.bpmn.xml",
            PROC_ID,
            BpmnProcessDefActions.DEPLOY
        )
    }

    @Test
    fun `running bpmn process multiple time on one flow should fill element end time correct`() {
        bpmnProcessService.startProcess(
            StartProcessRequest(
                PROC_ID,
                null,
                emptyMap()
            )
        )

        Awaitility.await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val foundBpmnElements = recordsService.query(
                RecordsQuery.create {
                    withSourceId(BPMN_ELEMENTS_REPO_SOURCE_ID)
                    withQuery(
                        Predicates.and(
                            Predicates.eq(BPMN_ELEMENT_DEF_ID, "Activity_script_increment_counter")
                        )
                    )
                },
                BpmnElementData::class.java
            ).getRecords()

            assertThat(foundBpmnElements).hasSize(5)
            assertThat(foundBpmnElements).allMatch { it.endTime != null }
        }
    }

    @AfterAll
    fun tearDown() {
        cleanDeployments()
        cleanDefinitions()
    }

    data class BpmnElementData(
        @AttName("completed")
        var endTime: Instant?
    )
}
