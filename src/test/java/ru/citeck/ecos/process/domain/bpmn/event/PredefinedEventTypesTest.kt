package ru.citeck.ecos.process.domain.bpmn.event

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_PROCESS_START
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_COMPLETE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_COMPLETE_ERROR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_CREATE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.BPMN_EVENT_USER_TASK_DELETE
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
 * The seven event types added on top of [EcosEventType.USER_TASK_ASSIGN], each of which is already
 * published on the bus by the platform and could not be subscribed to declaratively before.
 *
 * Two things are pinned here, and only the second one is cheap to change later. The first is the
 * plumbing: a wire name and an enum name both resolve to the type, a subscription is registered under
 * the wire name and composed under the enum name. The second is the `defaultModel` — the attributes
 * every subscriber of the type receives whether it asked for them or not. Renaming or dropping a key
 * there breaks subscriptions that are already deployed, so the composition of each model is asserted
 * exactly rather than "contains".
 */
class PredefinedEventTypesTest {

    companion object {

        @JvmStatic
        fun addedTypes(): List<Array<Any>> = listOf(
            arrayOf(EcosEventType.RECORD_DRAFT_STATUS_CHANGED, "record-draft-status-changed"),
            arrayOf(EcosEventType.RECORD_CONTENT_CHANGED, "record-content-changed"),
            arrayOf(EcosEventType.USER_TASK_CREATE, BPMN_EVENT_USER_TASK_CREATE),
            arrayOf(EcosEventType.USER_TASK_COMPLETE, BPMN_EVENT_USER_TASK_COMPLETE),
            arrayOf(EcosEventType.USER_TASK_COMPLETE_ERROR, BPMN_EVENT_USER_TASK_COMPLETE_ERROR),
            arrayOf(EcosEventType.USER_TASK_DELETE, BPMN_EVENT_USER_TASK_DELETE),
            arrayOf(EcosEventType.PROCESS_START, BPMN_EVENT_PROCESS_START)
        )
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `platform event name is resolved to the event type`(type: EcosEventType, eventName: String) {
        assertThat(EcosEventType.from(eventName)).isEqualTo(type)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `event type name is resolved to the event type`(type: EcosEventType, eventName: String) {
        assertThat(EcosEventType.from(type.name)).isEqualTo(type)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `type is subscribed on exactly one platform event name`(type: EcosEventType, eventName: String) {
        assertThat(type.availableEventNames()).containsExactly(eventName)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `representation is found by the platform event name`(type: EcosEventType, eventName: String) {
        val representation = EcosEventType.findRepresentation(eventName)

        assertThat(representation).isNotNull
        assertThat(representation!!.eventName).isEqualTo(eventName)
        assertThat(representation.defaultModel)
            .containsExactlyInAnyOrderEntriesOf(type.eventRepresentations.single().defaultModel)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `incoming event is composed into the document scoped names`(type: EcosEventType, eventName: String) {
        val document = EntityRef.valueOf("store/doc@1")
        val docType = EntityRef.valueOf("emodel/type@doc")

        val composedEventNames = ComposedEventNameGenerator.generateFromIncomingEcosEvent(
            IncomingEventData(eventName, document, docType)
        )

        assertThat(composedEventNames).containsExactlyInAnyOrder(
            ComposedEventName(type.name),
            ComposedEventName(type.name, ComposedEventName.RECORD_ANY, docType.toString()),
            ComposedEventName(type.name, document.toString()),
            ComposedEventName(type.name, document.toString(), docType.toString())
        )
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `combined subscription requests the default model attributes`(type: EcosEventType, eventName: String) {
        val subscription = EventSubscription(
            elementId = "signal_catch",
            name = ComposedEventName(type.name, "store/doc@1"),
            model = emptyMap()
        )

        val combined = EventSubscriptionCombiner.combine(listOf(subscription))

        assertThat(combined).containsExactly(
            CombinedEventSubscription(
                eventName = type.name,
                attributes = mutableSetOf<String>()
                    .addDefaultEventAtts()
                    .addDefaultAttsOfEventRepresentations(type.name)
                    .toSet()
            )
        )
        assertThat(combined.single().attributes)
            .containsAll(EcosEventType.findRepresentation(eventName)!!.defaultModel.values)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("addedTypes")
    fun `signal name of an event definition is composed from the event type`(
        type: EcosEventType,
        eventName: String
    ) {
        val eventDef = BpmnSignalEventDef(
            id = "SignalEventDefinition_1",
            eventType = type,
            eventFilterByRecordType = FilterEventByRecord.ANY
        )

        assertThat(eventDef.signalName).startsWith(
            "${type.name};${ComposedEventName.RECORD_ANY};${ComposedEventName.TYPE_ANY};"
        )
    }

    @Test
    fun `draft status change carries both sides as booleans`() {
        // `?bool` and not a bare attribute: bare is read through `?disp` and yields the strings
        // "true"/"false". BpmnProcessAutoStarter filters this very event with eq("after?bool", false).
        assertThat(EcosEventType.findRepresentation("record-draft-status-changed")!!.defaultModel)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "before" to "before?bool",
                    "after" to "after?bool"
                )
            )
    }

    @Test
    fun `content change carries the file name on both sides`() {
        // before/after are content AttValues whose getDisplayName is the file name with extension,
        // so add / replace / delete are told apart by which side is blank.
        assertThat(EcosEventType.findRepresentation("record-content-changed")!!.defaultModel)
            .containsExactlyInAnyOrderEntriesOf(
                emptyMap()
            )
    }

    @Test
    fun `user task create carries the recipients the task was offered to`() {
        assertThat(EcosEventType.findRepresentation(BPMN_EVENT_USER_TASK_CREATE)!!.defaultModel)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "assigneeRef" to "assigneeRef?id",
                    "candidateUsersRef" to "candidateUsersRef[]?id",
                    "candidateGroupsRef" to "candidateGroupsRef[]?id"
                )
            )
    }

    @Test
    fun `user task complete carries the outcome id and the actor`() {
        // the outcome ID routes; outcomeName is a display label and stays out of the model.
        assertThat(EcosEventType.findRepresentation(BPMN_EVENT_USER_TASK_COMPLETE)!!.defaultModel)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "outcome" to "outcome",
                    "completedBy" to "completedBy",
                    "comment" to "comment",
                    "assigneeRef" to "assigneeRef?id"
                )
            )
    }

    @Test
    fun `user task complete error carries the message but never the stack trace`() {
        val defaultModel = EcosEventType.findRepresentation(BPMN_EVENT_USER_TASK_COMPLETE_ERROR)!!.defaultModel

        assertThat(defaultModel).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "taskId" to "taskId?id",
                "elementDefId" to "elementDefId",
                "outcome" to "outcome",
                "completedBy" to "completedBy",
                "errorMessage" to "errorMessage"
            )
        )
        assertThat(defaultModel.values).doesNotContain("errorStackTrace")
    }

    @Test
    fun `user task delete carries the holder the task was taken away from`() {
        assertThat(EcosEventType.findRepresentation(BPMN_EVENT_USER_TASK_DELETE)!!.defaultModel)
            .containsExactlyInAnyOrderEntriesOf(
                mapOf(
                    "taskId" to "taskId?id",
                    "elementDefId" to "elementDefId",
                    "assigneeRef" to "assigneeRef?id"
                )
            )
    }

    @Test
    fun `process start carries the stable process key and not the camunda definition id`() {
        val defaultModel = EcosEventType.findRepresentation(BPMN_EVENT_PROCESS_START)!!.defaultModel

        assertThat(defaultModel).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "processKey" to "processKey",
                "processInstanceId" to "processInstanceId",
                "document" to "document?id"
            )
        )
        assertThat(defaultModel.values).doesNotContain("processDefinitionId")
    }
}
