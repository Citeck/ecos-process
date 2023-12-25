package ru.citeck.ecos.process.domain.bpmnla.dto

data class LazyApprovalResponseToNotificationDto(
    val taskId: String,
    val token: String,
    val outcome: String,
    val comment: String,
    val type: ResponseToNotificationType,
    val email: String?
)

enum class ResponseToNotificationType {
    EMAIL
}

