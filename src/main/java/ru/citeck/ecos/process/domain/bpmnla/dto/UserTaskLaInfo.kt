package ru.citeck.ecos.process.domain.bpmnla.dto

import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.records2.RecordRef

data class UserTaskLaInfo(
    val laEnabled: Boolean,
    val laNotificationType: NotificationType?,
    val laNotificationTemplate: RecordRef?
)
