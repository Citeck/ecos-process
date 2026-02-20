package ru.citeck.ecos.process.domain.bpmn.process

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.context.auth.RunAsSystem

@Component
class BpmnProcessDefFinder(
    private val recordsService: RecordsService,
    private val bpmnProcessService: BpmnProcessService,
    private val procDefService: ProcDefService,
    private val workspaceService: WorkspaceService
) {

    @RunAsSystem
    fun getByBpmnDefEngine(bpmnDefEngine: EntityRef): EntityRef {
        return EntityRef.valueOf(recordsService.getAtt(bpmnDefEngine, "ecosDefRev.processDefRef?id").asText())
    }

    @RunAsSystem
    fun getByProcessInstanceId(processInstanceId: String): EntityRef {
        if (processInstanceId.isBlank()) {
            return EntityRef.EMPTY
        }

        val processDefinition =
            bpmnProcessService.getProcessDefinitionByProcessInstanceId(processInstanceId) ?: return EntityRef.EMPTY
        val deploymentId = processDefinition.deploymentId ?: return EntityRef.EMPTY
        return getByDeploymentId(deploymentId)
    }

    @RunAsSystem
    fun getByDeploymentId(deploymentId: String): EntityRef {
        if (deploymentId.isBlank()) {
            return EntityRef.EMPTY
        }

        val defRev = procDefService.getProcessDefRevByDeploymentId(deploymentId) ?: return EntityRef.EMPTY
        val procDefId = defRev.procDefId
        if (procDefId.isBlank()) {
            return EntityRef.EMPTY
        }
        val localId = workspaceService.addWsPrefixToId(procDefId, defRev.workspace)
        return EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, localId)
    }

    @RunAsSystem
    fun getByProcessKey(processKey: String): EntityRef {
        val normalizedKey = processKey.replace(ProcUtils.PROC_KEY_WS_DELIM, IdInWs.WS_DELIM)
        val idInWs = workspaceService.convertToIdInWs(normalizedKey)
        val procDef =
            procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, idInWs)) ?: return EntityRef.EMPTY
        val localId = workspaceService.addWsPrefixToId(procDef.id, procDef.workspace)
        return EntityRef.create(AppName.EPROC, BpmnProcessDefRecords.ID, localId)
    }
}
