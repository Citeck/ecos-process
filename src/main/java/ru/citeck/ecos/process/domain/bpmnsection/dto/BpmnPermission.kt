package ru.citeck.ecos.process.domain.bpmnsection.dto

enum class BpmnPermission(val id: String) {
    READ("read"),
    WRITE("write"),

    PROC_DEF_DEPLOY("bpmn-process-def-deploy"),
    PROC_DEF_REPORT_VIEW("bpmn-process-def-report-view"),

    SECTION_CREATE_PROC_DEF("bpmn-section-create-process-def"),
    SECTION_CREATE_SUBSECTION("bpmn-section-create-subsection"),
    SECTION_EDIT_PROC_DEF("bpmn-section-edit-process-def"),

    PROC_INSTANCE_READ("bpmn-process-instance-read"),
    PROC_INSTANCE_EDIT("bpmn-process-instance-edit"),
    PROC_INSTANCE_MIGRATE("bpmn-process-instance-migrate"),
    PROC_INSTANCE_RUN("bpmn-process-instance-run");

    fun getAll(): Set<String> {
        return values().mapTo(LinkedHashSet()) { it.id }
    }

    fun getAttribute(): String {
        return "permissions._has.$id?bool!"
    }
}
