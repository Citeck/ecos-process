package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.script

import org.camunda.bpm.engine.impl.bpmn.behavior.TaskActivityBehavior
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ProcessDefinitionImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

@Component
class ScriptTaskParseListener(
    private val workspaceService: WorkspaceService,
    private val procDefService: ProcDefService
) : AbstractBpmnParseListener() {

    override fun parseScriptTask(element: Element, scope: ScopeImpl, activity: ActivityImpl) {
        val currentBehavior = activity.activityBehavior
        if (currentBehavior != null) {
            activity.activityBehavior = CustomScriptTaskActivityBehaviour(
                currentBehavior,
                getProcWorkspace(activity.processDefinition)
            )
        }
    }

    private fun getProcWorkspace(definition: ProcessDefinitionImpl): String {
        var workspace = ProcUtils.getContextWorkspace()
        if (workspace.isNotBlank()) {
            return workspace
        }
        workspace = procDefService.getProcessDefRevByDeploymentId(definition.deploymentId)?.workspace
            ?: error("Process definition revision doesn't found by deployment id ${definition.deploymentId}")
        return workspace
    }

    private inner class CustomScriptTaskActivityBehaviour(
        private val impl: ActivityBehavior,
        private val workspace: String
    ) : TaskActivityBehavior() {
        override fun execute(execution: ActivityExecution) {
            if (workspace.isBlank()) {
                AuthContext.runAsSystem {
                    impl.execute(execution)
                }
            } else {
                workspaceService.runAsWsSystem(workspace) {
                    impl.execute(execution)
                }
            }
        }
    }
}
