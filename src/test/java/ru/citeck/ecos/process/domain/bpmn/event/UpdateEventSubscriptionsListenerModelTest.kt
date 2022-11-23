package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.deleteAllProcDefinitions
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class UpdateEventSubscriptionsListenerModelTest {

    @Autowired
    private lateinit var eventsService: EventsService

    @AfterEach
    fun clearDefinitions() {
        deleteAllProcDefinitions()
    }

    @Test
    fun `publish same events with different models should update model in ecos events listeners`() {
        val processModel = "test-process-with-events-model"
        val processModel2 = "test-process-with-events-model-2"

        saveAndDeployBpmn(SUBSCRIPTION, processModel)
        val firstDeployListeners = eventsService.getListeners()

        assertThat(firstDeployListeners["signal-name-1"]!!.attributes).containsExactlyInAnyOrder("value1", "value2")
        assertThat(firstDeployListeners["signal-name-2"]!!.attributes).containsExactlyInAnyOrder(
            "value3",
            "value4",
            "value3_1"
        )

        saveAndDeployBpmn(SUBSCRIPTION, processModel2)
        val secondDeployListeners = eventsService.getListeners()

        assertThat(secondDeployListeners["signal-name-1"]!!.attributes).containsExactlyInAnyOrder(
            "value1",
            "value2",
            "value2_1"
        )
        assertThat(secondDeployListeners["signal-name-2"]!!.attributes).containsExactlyInAnyOrder(
            "value3",
            "value4",
            "value3_1",
            "value3_2",
            "value4_1",
            "value5"
        )
        assertThat(secondDeployListeners["signal-name-3"]!!.attributes).containsExactlyInAnyOrder("value1")
    }
}
