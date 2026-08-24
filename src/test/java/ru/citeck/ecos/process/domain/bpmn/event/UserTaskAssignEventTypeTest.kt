package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_ASSIGN
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.CombinedEventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventName
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.ComposedEventNameGenerator
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscription
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EventSubscriptionCombiner
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.IncomingEventData
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.FilterEventByRecord
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * [EcosEventType.USER_TASK_ASSIGN] makes the moment when a user takes a task observable declaratively:
 * the signal of the platform event `bpmn-user-task-assign` must resolve into a bpmn event subscription
 * exactly like every other predefined event type.
 */
class UserTaskAssignEventTypeTest {

    @Test
    fun `ecos event name of user task assign is resolved to the event type`() {
        assertThat(EcosEventType.from(BPMN_EVENT_USER_TASK_ASSIGN))
            .isEqualTo(EcosEventType.USER_TASK_ASSIGN)
    }

    @Test
    fun `event type name of user task assign is resolved to the event type`() {
        assertThat(EcosEventType.from(EcosEventType.USER_TASK_ASSIGN.name))
            .isEqualTo(EcosEventType.USER_TASK_ASSIGN)
    }

    @Test
    fun `user task assign is subscribed on the platform event name`() {
        assertThat(EcosEventType.USER_TASK_ASSIGN.availableEventNames())
            .containsExactly(BPMN_EVENT_USER_TASK_ASSIGN)
    }

    @Test
    fun `default model of user task assign carries the actor and the task`() {
        val representation = EcosEventType.findRepresentation(BPMN_EVENT_USER_TASK_ASSIGN)

        assertThat(representation).isNotNull
        assertThat(representation!!.defaultModel).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "taskId" to "taskId?id",
                "assigneeRef" to "assigneeRef?id",
                "elementDefId" to "elementDefId"
            )
        )
    }

    @Test
    fun `incoming user task assign event generates composed names with the event type name`() {
        val document = EntityRef.valueOf("store/doc@1")
        val type = EntityRef.valueOf("emodel/type@doc")

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(
            IncomingEventData(BPMN_EVENT_USER_TASK_ASSIGN, document, type)
        )

        val eventName = EcosEventType.USER_TASK_ASSIGN.name
        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(eventName),
            ComposedEventName(eventName, ComposedEventName.RECORD_ANY, type.toString()),
            ComposedEventName(eventName, document.toString()),
            ComposedEventName(eventName, document.toString(), type.toString())
        )
    }

    @Test
    fun `combined subscription of user task assign requests the default model attributes`() {
        val subscription = EventSubscription(
            elementId = "signal_catch",
            name = ComposedEventName(EcosEventType.USER_TASK_ASSIGN.name, "store/doc@1"),
            model = mapOf("userAtt" to "assignee")
        )

        val combined = EventSubscriptionCombiner.combine(listOf(subscription))

        assertThat(combined).containsExactly(
            CombinedEventSubscription(
                eventName = EcosEventType.USER_TASK_ASSIGN.name,
                attributes = mutableSetOf("assignee")
                    .addDefaultEventAtts()
                    .addDefaultAttsOfEventRepresentations(EcosEventType.USER_TASK_ASSIGN.name)
                    .toSet()
            )
        )
    }

    @Test
    fun `signal name of a user task assign event definition is composed from the event type`() {
        val eventDef = BpmnSignalEventDef(
            id = "SignalEventDefinition_1",
            eventType = EcosEventType.USER_TASK_ASSIGN,
            eventFilterByRecordType = FilterEventByRecord.ANY
        )

        assertThat(eventDef.signalName).startsWith(
            "${EcosEventType.USER_TASK_ASSIGN.name};${ComposedEventName.RECORD_ANY};${ComposedEventName.TYPE_ANY};"
        )
    }
}
