package ru.citeck.ecos.process.domain.bpmn.service

import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.springframework.stereotype.Service
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

@Service
class BpmnProcServiceImpl(
    private val camundaRuntimeService: RuntimeService,
    private val camundaRepositoryService: RepositoryService,
    private val procDefService: ProcDefService
) : BpmnProcService {

    // TODO: remove Process_ prefix?
    // TODO: support _ECM_ document variables with doc mutate и отображение на карточке документа?
    override fun startProcess(processKey: String, variables: Map<String, Any?>): ProcessInstance {
        val definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, processKey))
            ?: throw IllegalArgumentException("Process definition with key $processKey not found")

        if (!definition.enabled) throw IllegalStateException("Starting a disabled process is not possible")

        return camundaRuntimeService.startProcessInstanceByKey("Process_${processKey}", variables)
    }

    override fun getProcessInstance(processInstanceId: String): ProcessInstance? {
        return camundaRuntimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult()
    }

    override fun getProcessDefinitionByInstanceId(processInstanceId: String): ProcessDefinition? {
        val processInstance = getProcessInstance(processInstanceId) ?: return null
        return getProcessDefinition(processInstance.processDefinitionId)
    }

    override fun getProcessDefinition(processDefinitionId: String): ProcessDefinition? {
        return camundaRepositoryService.getProcessDefinition(processDefinitionId)
    }
}
