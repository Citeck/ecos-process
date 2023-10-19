package ru.citeck.ecos.process.domain.bpmnreport.model

enum class ElementType(val flowElementType: String, val type: String) {
    STATUS("bpmn:BpmnTask", "Status"),

    EXCLUSIVE_GATEWAY("bpmn:BpmnExclusiveGateway", "Exclusive Gateway"),
    PARALLEL_GATEWAY("bpmn:BpmnParallelGateway", "Parallel Gateway"),
    INCLUSIVE_GATEWAY("bpmn:BpmnInclusiveGateway", "Inclusive Gateway"),
    EVENT_BASED_GATEWAY("bpmn:BpmnEventBasedGateway", "Event Based Gateway"),

    START_EVENT("bpmn:BpmnStartEvent", "Start Event"),
    END_EVENT("bpmn:BpmnEndEvent", "End Event"),
    INTERMEDIATE_THROW_EVENT("bpmn:BpmnIntermediateThrowEvent", "Intermediate Throw Event"),
    INTERMEDIATE_CATCH_EVENT("bpmn:BpmnIntermediateCatchEvent", "Intermediate Catch Event"),
    BOUNDARY_EVENT("bpmn:BpmnBoundaryEvent", "Boundary Event"),

    USER_TASK("bpmn:BpmnUserTask", "User Task"),
    SCRIPT_TASK("bpmn:BpmnScriptTask", "Script Task"),
    SEND_TASK("bpmn:BpmnSendTask", "Send Task"),
    BUSINESS_RULE_TASK("bpmn:BpmnBusinessRuleTask", "Business Rule Task"),
    SERVICE_TASK("bpmn:BpmnServiceTask", "Service Task"),

    SUB_PROCESS("bpmn:BpmnSubProcess", "Sub Process"),
    CALL_ACTIVITY("bpmn:BpmnCallActivity", "Call Activity")
}
