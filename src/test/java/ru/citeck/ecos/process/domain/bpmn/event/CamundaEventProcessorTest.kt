package ru.citeck.ecos.process.domain.bpmn.event

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.*
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventExploder
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.CamundaEventProcessor
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.toEventMeta
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.toIncomingEventData
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
internal class CamundaEventProcessorTest {

    @Autowired
    private lateinit var camundaEventProcessor: CamundaEventProcessor

    @MockBean
    private lateinit var camundaEventExploder: CamundaEventExploder

    @SpyBean
    private lateinit var camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder

    @Test
    fun `event processor model event meta attributes test`() {
        val subscriptionId = UUID.randomUUID().toString()

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = "some-ref"
                ),
                model = emptyMap()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames()[0],
            user = "ivan",
            attributes = emptyMap()
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    EVENT_META_ATT to incomingEvent.toEventMeta()
                )
            )
        )
    }

    @Test
    fun `event processor model attributes test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = mapOf("comment" to "text")
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames()[0],
            user = "ivan",
            attributes = mapOf("text" to DataValue.create(commentValue))
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "comment" to commentValue,
                    "text" to commentValue,
                    EVENT_META_ATT to incomingEvent.toEventMeta()
                )
            )
        )
    }

    @Test
    fun `event processor default comment model attributes test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"
        val commentRecord = "store/comment@1"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = emptyMap()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = "comment-create",
            user = "ivan",
            attributes = mapOf(
                "text" to DataValue.create(commentValue),
                "commentRecord?id" to DataValue.create(commentRecord)
            )
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "text" to commentValue,
                    "commentRecord" to commentRecord,
                    EVENT_META_ATT to incomingEvent.toEventMeta()
                )
            )
        )
    }

    @Test
    fun `event processor default comment model of second representation should maps to generic attributes`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"
        val commentRecord = "store/comment@1"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = emptyMap()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = "ecos.comment.create",
            user = "ivan",
            attributes = mapOf(
                "textAfter" to DataValue.create(commentValue),
                "commentRec?id" to DataValue.create(commentRecord)
            )
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "text" to commentValue,
                    "commentRecord" to commentRecord,
                    EVENT_META_ATT to incomingEvent.toEventMeta()
                )
            )
        )
    }

    @Test
    fun `event processor user model should be override default comment model attributes`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"
        val commentRecord = "store/comment@1"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = mapOf("text" to "commentRecord?id")
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = "comment-create",
            user = "ivan",
            attributes = mapOf(
                "text" to DataValue.create(commentValue),
                "commentRecord?id" to DataValue.create(commentRecord)
            )
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "text" to commentRecord,
                    "commentRecord" to commentRecord,
                    EVENT_META_ATT to incomingEvent.toEventMeta()
                )
            )
        )
    }

    @Test
    fun `event processor predicates false test`() {
        val subscriptionId = UUID.randomUUID().toString()

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = mapOf("comment" to "text"),
                predicate = Json.mapper.toString(Predicates.alwaysFalse())
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames()[0],
            user = "ivan",
            attributes = mapOf("text" to DataValue.create("its comment"))
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder, never()).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "comment" to "its comment"
                )
            )
        )
    }

    @Test
    fun `event processor predicates based on event atts test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = mapOf("comment" to "text"),
                predicate = """
                {
                    "att": "comment",
                    "val": "$commentValue",
                    "t": "eq"
                }
                """.trimIndent()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames()[0],
            user = "ivan",
            attributes = mapOf("text" to DataValue.create(commentValue))
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "comment" to commentValue,
                    "text" to commentValue,
                    EVENT_META_ATT to incomingEvent.toEventMeta()
                )
            )
        )
    }

    @Test
    fun `event processor predicates based on event atts false test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"
        val anotherCommentValue = "another comment"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = ComposedEventName.RECORD_ANY
                ),
                model = mapOf("comment" to "text"),
                predicate = """
                {
                    "att": "comment",
                    "val": "$commentValue",
                    "t": "eq"
                }
                """.trimIndent()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames()[0],
            user = "ivan",
            attributes = mapOf("text" to DataValue.create(anotherCommentValue))
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder, never()).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "comment" to commentValue
                )
            )
        )
    }
}
