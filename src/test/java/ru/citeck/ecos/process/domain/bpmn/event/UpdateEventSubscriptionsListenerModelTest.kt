package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.deleteAllProcDefinitions
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class UpdateEventSubscriptionsListenerModelTest {

    @Autowired
    private lateinit var eventsService: EventsService

    @BeforeEach
    fun clear() {
        deleteAllProcDefinitions()

        eventsService.getListeners().forEach {
            it.value.listeners.forEach { listener ->
                eventsService.removeListener(listener.config.id)
            }
        }
    }

    @Test
    fun `publish same events with different models should update model in ecos events listeners`() {
        val processModel = "test-process-with-events-model"
        val processModel2 = "test-process-with-events-model-2"

        saveAndDeployBpmn(SUBSCRIPTION, processModel)
        val firstDeployListeners = eventsService.getListeners()

        assertThat(firstDeployListeners["signal-name-a1"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf(
                "value1",
                "value2"
            ).addDefaultEventAtts()
        )
        assertThat(firstDeployListeners["signal-name-a2"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf(
                "value3",
                "value4",
                "value3_1"
            ).addDefaultEventAtts()
        )

        saveAndDeployBpmn(SUBSCRIPTION, processModel2)
        val secondDeployListeners = eventsService.getListeners()

        assertThat(secondDeployListeners["signal-name-a1"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf(
                "value1",
                "value2",
                "value2_1"
            ).addDefaultEventAtts()
        )
        assertThat(secondDeployListeners["signal-name-a2"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf(
                "value3",
                "value4",
                "value3_1",
                "value3_2",
                "value4_1",
                "value5"
            ).addDefaultEventAtts()
        )
        assertThat(secondDeployListeners["signal-name-a3"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf("value1").addDefaultEventAtts()
        )
    }

    @Test
    fun `publish existing type events with different models and multiple names should update model in ecos events listeners`() {
        val processModel = "process-with-events-model-with-existing-type"
        val processModel2 = "process-with-events-model-with-existing-type-2"

        saveAndDeployBpmn(SUBSCRIPTION, processModel)
        val firstDeployListeners = eventsService.getListeners()

        assertThat(firstDeployListeners["signal-name-ex1"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf("value1", "value2").addDefaultEventAtts()
        )

        EcosEventType.COMMENT_CREATE.availableEventNames.forEach { eventName ->
            assertThat(firstDeployListeners[eventName]!!.attributes).containsExactlyInAnyOrderElementsOf(
                listOf(
                    "commentValue1",
                    "commentValue1_1",
                    "commentValue2",
                    "commentValue3"
                ).addDefaultEventAtts()
            )
        }

        saveAndDeployBpmn(SUBSCRIPTION, processModel2)
        val secondDeployListeners = eventsService.getListeners()

        assertThat(secondDeployListeners["signal-name-ex1"]!!.attributes).containsExactlyInAnyOrderElementsOf(
            listOf(
                "value1",
                "value2",
                "value2_1"
            ).addDefaultEventAtts()
        )
        EcosEventType.COMMENT_CREATE.availableEventNames.forEach { eventName ->
            assertThat(secondDeployListeners[eventName]!!.attributes).containsExactlyInAnyOrderElementsOf(
                listOf(
                    "commentValue1",
                    "commentValue1_1",
                    "commentValue2",
                    "commentValue3",
                    "commentValue3_2",
                    "commentValue4_1",
                    "commentValue5"
                ).addDefaultEventAtts()
            )
        }
    }
}
