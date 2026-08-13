package ru.citeck.ecos.process.domain.bpmn.model.ecos.task

/**
 * Strategy for distributing the "To" field recipients across BPMN Send Task emails.
 * An ecos-process-owned enum (the library RecipientsSendStrategy is not extensible).
 *
 * The value is serialized as a string into the BPMN attribute notificationRecipientsStrategy.
 * Renaming/removing constants breaks the import of existing processes.
 */
enum class BpmnRecipientsSendStrategy {

    /**
     * A single email to all recipients (default).
     */
    COMBINED,

    /**
     * A separate email per recipient.
     */
    PER_RECIPIENT,

    /**
     * A separate email per role (all of its recipients together).
     */
    PER_ROLE
}
