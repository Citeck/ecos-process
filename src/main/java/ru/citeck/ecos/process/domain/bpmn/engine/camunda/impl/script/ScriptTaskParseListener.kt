package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.script

import com.github.benmanes.caffeine.cache.Caffeine
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.impl.bpmn.behavior.TaskActivityBehavior
import org.camunda.bpm.engine.impl.bpmn.parser.AbstractBpmnParseListener
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityBehavior
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl
import org.camunda.bpm.engine.impl.util.xml.Element
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import java.util.concurrent.TimeUnit

@Component
class ScriptTaskParseListener(
    private val workspaceService: WorkspaceService,
    private val procDefService: ProcDefService
) : AbstractBpmnParseListener() {

    private lateinit var camundaRepoService: RepositoryService
    private val workspaceByDeploymentIdCache = Caffeine.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .maximumSize(1000)
        .build<String, String> {
            AuthContext.runAsSystem {
                getProcWorkspaceImpl(it)
            }
        }

    override fun parseScriptTask(element: Element, scope: ScopeImpl, activity: ActivityImpl) {
        val currentBehavior = activity.activityBehavior
        if (currentBehavior != null) {
            activity.activityBehavior = CustomScriptTaskActivityBehaviour(
                currentBehavior,
                workspaceByDeploymentIdCache.get(activity.processDefinition.deploymentId)
            )
        }
    }

    private fun getProcWorkspaceImpl(deploymentId: String): String {
        var workspace = ProcUtils.getDeployContextWorkspace()
        if (workspace != null) return workspace
        workspace = procDefService.getProcessDefRevByDeploymentId(deploymentId)?.workspace
        if (workspace != null) return workspace

        val procDefKeyInCamunda = camundaRepoService.createProcessDefinitionQuery()
            .deploymentId(deploymentId)
            .singleResult()?.key

        if (procDefKeyInCamunda.isNullOrBlank()) {
            error("Process definition doesn't found for deploymentId: $deploymentId")
        }

        return if (procDefKeyInCamunda.contains(ProcUtils.PROC_KEY_WS_DELIM)) {
            val workspaceSysId = procDefKeyInCamunda.substringBefore(ProcUtils.PROC_KEY_WS_DELIM)
            val workspaceId = workspaceService.getWorkspaceIdBySystemId(workspaceSysId)
            if (workspaceId.isBlank()) {
                error(
                    "Workspace doesn't found for systemId: '$workspaceSysId'. " +
                    "ProcKey: '${procDefKeyInCamunda}' DeploymentId: '$deploymentId'"
                )
            }
            workspaceId
        } else {
            ""
        }
    }

    private inner class CustomScriptTaskActivityBehaviour(
        private val impl: ActivityBehavior,
        private val workspace: String
    ) : TaskActivityBehavior() {

        override fun execute(execution: ActivityExecution) {
            if (workspaceService.isWorkspaceWithGlobalEntities(workspace)) {
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

    @Lazy
    @Autowired
    fun setCamundaRepoService(camundaRepoService: RepositoryService) {
        this.camundaRepoService = camundaRepoService
    }
}
