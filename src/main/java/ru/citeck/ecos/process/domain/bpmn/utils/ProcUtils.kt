package ru.citeck.ecos.process.domain.bpmn.utils

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.workspace.WorkspaceService

@Component
class ProcUtils(
    val workspaceService: WorkspaceService
) {

    companion object {
        const val PROC_KEY_WS_DELIM = ".."
        const val WS_REF_PREFIX = "emodel/workspace@"
    }

    fun getUpdatedWsInMutation(currentWs: String, ctxWorkspace: String?): String {
        if (currentWs.isNotBlank() ||
            ctxWorkspace.isNullOrBlank() ||
            workspaceService.isWorkspaceWithGlobalArtifacts(ctxWorkspace)
        ) {
            return currentWs
        }
        return ctxWorkspace.replace(WS_REF_PREFIX, "")
    }

}
