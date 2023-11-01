package ru.citeck.ecos.process.domain.bpmn.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BPMN_PROCESS_DEF_RECORDS_SOURCE_ID
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
    private val procDefService: ProcDefService
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

        return EntityRef.create(AppName.EPROC, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, procDefId)
    }

    @RunAsSystem
    fun getByProcessKey(processKey: String): EntityRef {
        val procDef =
            procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, processKey)) ?: return EntityRef.EMPTY
        return EntityRef.create(AppName.EPROC, BPMN_PROCESS_DEF_RECORDS_SOURCE_ID, procDef.id)
    }
}
