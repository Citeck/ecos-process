package ru.citeck.ecos.process.domain.bpmn.engine.camunda

const val BPMN_DOCUMENT = "document"
const val BPMN_DOCUMENT_REF = "documentRef"
const val BPMN_DOCUMENT_TYPE = "documentType"
const val BPMN_BUSINESS_KEY = "execution.businessKey"
const val BPMN_EVENT = "event"
const val BPMN_NAME_ML = "nameMl"
const val BPMN_POSSIBLE_OUTCOMES = "possibleOutcomes"

const val BPMN_COMMENT = "comment"

const val BPMN_TASK_COMMENT_LOCAL = "taskComment"
const val BPMN_LAST_COMMENT_LOCAL = "lastComment"

const val BPMN_LAST_TASK_COMPLETOR = "lastTaskCompletor"
const val BPMN_LAST_TASK_COMMENT = "lastTaskComment"

const val BPMN_WORKFLOW_INITIATOR = "workflowInitiator"
const val BPMN_TASK_COMPLETED_BY = "completedBy"
const val BPMN_TASK_COMPLETED_ON_BEHALF_OF = "completedOnBehalfOf"
const val BPMN_TASK_SENDER = "sender"
const val BPMN_TASK_CANDIDATES_USER_ORIGINAL = "candidatesUserOriginal"
const val BPMN_TASK_CANDIDATES_GROUP_ORIGINAL = "candidatesGroupOriginal"

const val BPMN_CAMUNDA_COLLECTION_SEPARATOR = ","

const val BPMN_ASSIGNEE_ELEMENT = "assigneeElement"

val DEFAULT_IN_VARIABLES_PROPAGATION_TO_CALL_ACTIVITY = listOf(
    BPMN_DOCUMENT_REF,
    BPMN_DOCUMENT_TYPE,
    BPMN_WORKFLOW_INITIATOR
)

const val BPMN_EXECUTION_ID = "executionId"
const val BPMN_PROCESS_INSTANCE_ID = "procInstanceId"
const val BPMN_ELEMENT_ID = "elementId"
const val BPMN_ELEMENT_DEF_ID = "elementDefId"
