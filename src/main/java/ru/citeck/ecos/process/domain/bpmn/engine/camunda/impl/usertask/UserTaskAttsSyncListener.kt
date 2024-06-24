package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.usertask

import org.camunda.bpm.engine.delegate.DelegateTask
import org.camunda.bpm.engine.delegate.TaskListener
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.addTaskListener
import ru.citeck.ecos.process.domain.proctask.attssync.ProcTaskAttsSynchronizer
import ru.citeck.ecos.records3.record.request.RequestContext

@Component
class UserTaskAttsSyncParseListener(
    private val userTaskAttsSyncListener: UserTaskAttsSyncListener
) : AbstractBpmnParseListener() {

    override fun parseUserTask(userTaskElement: Element, scope: ScopeImpl, activity: ActivityImpl) {
        activity.addTaskListener(TaskListener.EVENTNAME_CREATE, userTaskAttsSyncListener)
    }
}

@Component
class UserTaskAttsSyncListener(
    val procTaskAttsSynchronizer: ProcTaskAttsSynchronizer
) : TaskListener {

    override fun notify(delegateTask: DelegateTask) {
        AuthContext.runAsSystem {
            RequestContext.doWithTxn {
                procTaskAttsSynchronizer.syncOnTaskCreate(delegateTask)
            }
        }
    }
}
