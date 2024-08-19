package ru.citeck.ecos.process.domain.bpmnla.dto

import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.webapp.api.entity.EntityRef

data class UserTaskLaInfo(
    val laEnabled: Boolean = false,
    val laNotificationType: NotificationType? = null,
    val laNotificationTemplate: EntityRef? = null,
    val laManualNotificationTemplateEnabled: Boolean = false,
    val laManualNotificationTemplate: String? = null,
    val laReportEnabled: Boolean = false,
    val laSuccessReportNotificationTemplate: EntityRef? = null,
    val laErrorReportNotificationTemplate: EntityRef? = null
)
