package ru.citeck.ecos.process.domain.procdef.convert.io.convert.context

import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.model.ecos.error.BpmnErrorDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.signal.BpmnSignalDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.utils.BpmnUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverters

class ExportContext(
    val converters: EcosOmgConverters,
    val bpmnElementsById: MutableMap<String, TBaseElement> = mutableMapOf(),
    val bpmnSignalsByNames: MutableMap<String, BpmnSignalDef> = mutableMapOf(),
    val bpmnErrorsByNames: MutableMap<String, BpmnErrorDef> = mutableMapOf(),
    val cmmnElementsById: MutableMap<String, TCmmnElement> = mutableMapOf(),
    val cmmnPItemByDefId: MutableMap<String, TCmmnElement> = mutableMapOf(),
    val workspaceService: WorkspaceService
) {
    private var workspaceSysId = ""

    fun setWorkspace(workspace: String) {
        workspaceSysId = if (!workspaceService.isWorkspaceWithGlobalArtifacts(workspace)) {
            workspaceService.getWorkspaceSystemId(workspace)
        } else {
            ""
        }
    }

    fun createWsScopedId(id: String): String {
        if (workspaceSysId.isBlank()) {
            return id
        }
        return workspaceSysId + BpmnUtils.PROC_KEY_WS_DELIM + id
    }
}
