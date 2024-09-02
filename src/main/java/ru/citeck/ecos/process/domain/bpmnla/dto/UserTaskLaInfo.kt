package ru.citeck.ecos.process.domain.bpmnla.dto

import ru.citeck.ecos.notifications.lib.NotificationType
import ru.citeck.ecos.records2.RecordRef

data class UserTaskLaInfo(
    val laEnabled: Boolean = false,
    val laNotificationType: NotificationType? = null,
    val laNotificationTemplate: RecordRef? = null,
    val laManualNotificationTemplateEnabled: Boolean = false,
    val laManualNotificationTemplate: String? = null,
    val laNotificationAdditionalMeta: Map<String, String> = emptyMap(),
    val laReportEnabled: Boolean = false,
    val laSuccessReportNotificationTemplate: RecordRef? = null,
    val laErrorReportNotificationTemplate: RecordRef? = null
)
