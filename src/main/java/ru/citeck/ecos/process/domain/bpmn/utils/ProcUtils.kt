package ru.citeck.ecos.process.domain.bpmn.utils

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.workspace.WorkspaceService

@Component
class ProcUtils(
    val workspaceService: WorkspaceService
) {
    companion object {
        const val PROC_KEY_WS_DELIM = ".."

        private val contextWorkspace = ThreadLocal<String>()

        fun <T> doWithWorkspaceContext(workspace: String, action: () -> T): T {
            val wsBefore = contextWorkspace.get()
            contextWorkspace.set(workspace)
            try {
                return action.invoke()
            } finally {
                if (wsBefore == null) {
                    contextWorkspace.remove()
                } else {
                    contextWorkspace.set(wsBefore)
                }
            }
        }

        fun getContextWorkspace(): String? {
            return contextWorkspace.get()
        }
    }

    fun <T> runAsWsSystemIfRequiredForProcDef(procDefId: String?, action: () -> T): T {
        procDefId ?: return action()
        return if (procDefId.contains(PROC_KEY_WS_DELIM)) {
            val workspaceSysId = procDefId.substringBefore(PROC_KEY_WS_DELIM)
            workspaceService.runAsWsSystemBySystemId(workspaceSysId, action)
        } else {
            action()
        }
    }

    fun getUpdatedWsInMutation(currentWs: String, ctxWorkspace: String?): String {
        return workspaceService.getUpdatedWsInMutation(currentWs, ctxWorkspace)
    }
}
