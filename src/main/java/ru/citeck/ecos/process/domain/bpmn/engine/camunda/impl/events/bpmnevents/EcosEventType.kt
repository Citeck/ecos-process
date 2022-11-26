package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents


enum class EcosEventType(
    val availableEventNames: List<String>,
    val attsForFindRecord: List<String>
) {

    UNDEFINED(emptyList(), emptyList()),
    COMMENT_CREATE(listOf("ecos.comment.create", "comment-create"), listOf("record", "rec")),
    COMMENT_UPDATE(listOf("ecos.comment.update", "comment-update"), listOf("record", "rec")),
    COMMENT_DELETE(listOf("ecos.comment.delete", "comment-delete"), listOf("record", "rec"));

    companion object {
        fun from(value: String): EcosEventType =
            EcosEventType.values().find { it.availableEventNames.contains(value) } ?: let {
                EcosEventType.values().find { it.name == value } ?: UNDEFINED
            }
    }
}
