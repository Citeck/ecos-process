package ru.citeck.ecos.process.domain.bpmn.engine.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.Expression
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import ru.citeck.ecos.notifications.lib.Notification
import ru.citeck.ecos.notifications.lib.service.NotificationService
import ru.citeck.ecos.records2.RecordRef

@Component
class SendNotificationDelegate(
    private val notificationService: NotificationService
) : JavaDelegate {

    var notificationTemplate: Expression? = null
    var notificationRecord: Expression? = null
    var notificationTitle: Expression? = null
    var notificationBody: Expression? = null

    override fun execute(execution: DelegateExecution?) {

        val nt = Notification.Builder()
            .title(notificationTitle?.expressionText ?: "")
            .body(notificationBody?.expressionText ?: "")
            .templateRef(RecordRef.valueOf(notificationTemplate?.expressionText))
            .recipients(listOf("some-mail@mail.ru"))
            .build()

        notificationService.send(nt)

    }
}
