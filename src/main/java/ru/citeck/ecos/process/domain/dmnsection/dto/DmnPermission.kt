package ru.citeck.ecos.process.domain.dmnsection.dto

enum class DmnPermission(val id: String) {
    READ("read"),
    WRITE("write"),

    DMN_DEF_DEPLOY("dmn-def-deploy"),
    DMN_INSTANCE_EDIT("dmn-instance-edit"),
    SECTION_CREATE_DMN_DEF("dmn-section-create-dmn-def"),
    SECTION_EDIT_DMN_DEF("dmn-section-edit-dmn-def");

    fun getAll(): Set<String> {
        return values().mapTo(LinkedHashSet()) { it.id }
    }

    fun getAttribute(): String {
        return "permissions._has.$id?bool!"
    }
}
