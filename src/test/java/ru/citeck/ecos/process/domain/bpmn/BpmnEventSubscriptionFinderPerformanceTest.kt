package ru.citeck.ecos.process.domain.bpmn

import mu.KotlinLogging
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CamundaEventSubscriptionFinder
import ru.citeck.ecos.process.domain.saveAndDeployBpmnFromString
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.nio.charset.StandardCharsets

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnEventSubscriptionFinderPerformanceTest {

    @Autowired
    private lateinit var camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @BeforeAll
    fun setUp() {

        val processData = ResourceUtils.getFile("classpath:test/bpmn/large-test-process-start-event.bpmn.xml")
            .readText(StandardCharsets.UTF_8)
        val signalName = "signal-start-event-0"
        val procId = "large-test-process-start-event"

        for (i in 0 until 100) {
            val newId = "$procId-$i"

            val bpmnData = processData
                .replace(signalName, "signal-start-event-$i")
                .replace(procId, newId)

            log.info { "Deploying process $newId" }

            saveAndDeployBpmnFromString(bpmnData, newId)
        }
    }

    @Test
    fun `find all deployed subscriptions should go without a memory leak `() {
        camundaEventSubscriptionFinder.findAllDeployedSubscriptions()
    }
}
