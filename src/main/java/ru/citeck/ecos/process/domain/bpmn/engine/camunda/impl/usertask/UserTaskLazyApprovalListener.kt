package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmnla.services.BpmnLazyApprovalService

@Component
class UserTaskLazyApprovalListener(
    private val bpmnLazyApprovalService: BpmnLazyApprovalService
) : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        bpmnLazyApprovalService.sendNotification(delegateTask)
    }
}
