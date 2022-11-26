package ru.citeck.ecos.process.domain.bpmn.event

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.publish.toIncomingEventData
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.subscribe.GeneralEvent
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.source.dao.local.RecordsDaoBuilder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant
import java.util.UUID

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
internal class CamundaEventProcessorTest {

    @Autowired
    private lateinit var camundaEventProcessor: CamundaEventProcessor

    @Autowired
    private lateinit var recordsService: RecordsService

    @MockBean
    private lateinit var camundaEventExploder: CamundaEventExploder

    @SpyBean
    private lateinit var camundaEventSubscriptionFinder: CamundaEventSubscriptionFinder

    companion object {
        private val harryRecord = PotterRecord()
        private val harryRef = RecordRef.valueOf("hogwarts/people@harry")

    }

    @BeforeEach
    fun setUp() {
        recordsService.register(
            RecordsDaoBuilder.create("hogwarts/people")
                .addRecord(
                    harryRef.id,
                    PotterRecord()
                )
                .build()
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
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
                    "comment" to commentValue
                    //TODO: default event attributes?
                    /*"_id" to "event-1",
                    "_user" to "ivan",
                    "_type" to EcosEventType.COMMENT_CREATE.availableEventNames[0],
                    "_time" to eventTime
                    "_record to ?"
                    "_type to ?"
                    */
                )
            )
        )
    }

    @Test
    fun `event processor model record attributes test`() {
        val subscriptionId = UUID.randomUUID().toString()

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = harryRef.toString()
                ),
                model = mapOf(
                    "harryName" to "\$record.name",
                    "harryMail" to "\$record.email"
                )
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            record = harryRef.toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
                    "harryName" to harryRecord.name,
                    "harryMail" to harryRecord.email
                )
            )
        )
    }

    @Test
    fun `event processor model record and event attributes test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = harryRef.toString()
                ),
                model = mapOf(
                    "harryName" to "\$record.name",
                    "harryMail" to "\$record.email",
                    "comment" to "text"
                )
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            record = harryRef.toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
                    "harryName" to harryRecord.name,
                    "harryMail" to harryRecord.email,
                    "comment" to commentValue
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
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
                    "comment" to commentValue
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
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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

    @Test
    fun `event processor predicates based on record atts test`() {
        val subscriptionId = UUID.randomUUID().toString()

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = harryRef.toString()
                ),
                model = mapOf("harryName" to "\$record.name"),
                predicate = """
                {
                    "att": "harryName",
                    "val": "${harryRecord.name}",
                    "t": "eq"
                }
            """.trimIndent()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            record = harryRef.toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
                    "harryName" to harryRecord.name
                )
            )
        )
    }

    @Test
    fun `event processor predicates based on record atts false test`() {
        val subscriptionId = UUID.randomUUID().toString()

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = harryRef.toString()
                ),
                model = mapOf("harryName" to "\$record.name"),
                predicate = """
                {
                    "att": "harryName",
                    "val": "hermione",
                    "t": "eq"
                }
            """.trimIndent()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            record = harryRef.toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
            user = "ivan",
            attributes = emptyMap()
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder, never()).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "harryName" to harryRecord.name
                )
            )
        )
    }

    @Test
    fun `event processor predicates based on record and event atts test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = harryRef.toString()
                ),
                model = mapOf(
                    "harryName" to "\$record.name",
                    "comment" to "text"
                ),
                predicate = """
                    {
                        "t": "and",
                        "v": [
                            {
                                "t": "eq",
                                "att": "harryName",
                                "val": "${harryRecord.name}"
                            },
                            {
                                "t": "eq",
                                "att": "comment",
                                "val": "$commentValue"
                            }
                        ]
                    }
                """.trimIndent()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            record = harryRef.toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
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
                    "harryName" to harryRecord.name,
                    "comment" to commentValue
                )
            )
        )
    }

    @Test
    fun `event processor predicates based on record and event atts false test`() {
        val subscriptionId = UUID.randomUUID().toString()
        val commentValue = "its comment"

        val subscription = CamundaEventSubscription(
            id = subscriptionId,
            event = EventSubscription(
                name = ComposedEventName(
                    event = EcosEventType.COMMENT_CREATE.name,
                    record = harryRef.toString()
                ),
                model = mapOf(
                    "harryName" to "\$record.name",
                    "comment" to "text"
                ),
                predicate = """
                    {
                        "t": "and",
                        "v": [
                            {
                                "t": "eq",
                                "att": "harryName",
                                "val": "${harryRecord.name}"
                            },
                            {
                                "t": "not-eq",
                                "att": "comment",
                                "val": "$commentValue"
                            }
                        ]
                    }
                """.trimIndent()
            )
        )

        val incomingEvent = GeneralEvent(
            id = UUID.randomUUID().toString(),
            record = harryRef.toString(),
            time = Instant.now(),
            type = EcosEventType.COMMENT_CREATE.availableEventNames[0],
            user = "ivan",
            attributes = mapOf("text" to DataValue.create(commentValue))
        )

        `when`(camundaEventSubscriptionFinder.getActualCamundaSubscriptions(incomingEvent.toIncomingEventData()))
            .thenReturn(listOf(subscription))

        camundaEventProcessor.processEvent(incomingEvent)

        verify(camundaEventExploder, never()).fireEvent(
            subscriptionId,
            ObjectData.create(
                mapOf(
                    "harryName" to harryRecord.name,
                    "comment" to commentValue
                )
            )
        )
    }
}

class PotterRecord(

    @AttName("email")
    val email: String = "harry.potter@hogwarts.com",

    @AttName("name")
    val name: String = "Harry Potter"
)
