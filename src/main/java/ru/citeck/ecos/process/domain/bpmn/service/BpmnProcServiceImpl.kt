package ru.citeck.ecos.process.domain.bpmn.service

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.springframework.stereotype.Service
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_WORKFLOW_INITIATOR
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

@Service
class BpmnProcServiceImpl(
    private val camundaRuntimeService: RuntimeService,
    private val camundaRepositoryService: RepositoryService,
    private val procDefService: ProcDefService
) : BpmnProcService {

    override fun startProcess(processKey: String, businessKey: String?, variables: Map<String, Any?>): ProcessInstance {
        val definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, processKey))
            ?: throw IllegalArgumentException("Process definition with key $processKey not found")

        if (!definition.enabled) throw IllegalStateException("Starting a disabled process is not possible")

        val processVariables = variables.toMutableMap()
        processVariables[BPMN_WORKFLOW_INITIATOR] = AuthContext.getCurrentUser()

        return camundaRuntimeService.startProcessInstanceByKey(processKey, businessKey, processVariables)
    }

    override fun getProcessInstance(processInstanceId: String): ProcessInstance? {
        return camundaRuntimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult()
    }

    override fun getProcessDefinitionByProcessInstanceId(processInstanceId: String): ProcessDefinition? {
        val processInstance = getProcessInstance(processInstanceId) ?: return null
        return getProcessDefinition(processInstance.processDefinitionId)
    }

    override fun getProcessDefinition(processDefinitionId: String): ProcessDefinition? {
        return camundaRepositoryService.getProcessDefinition(processDefinitionId)
    }

    override fun getProcessDefinitionsByKey(processKey: String): List<ProcessDefinition> {
        return camundaRepositoryService.createProcessDefinitionQuery().processDefinitionKey(processKey).list().toList()
    }
}
