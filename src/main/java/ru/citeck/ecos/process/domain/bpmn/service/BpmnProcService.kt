package ru.citeck.ecos.process.domain.bpmn.service

import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.ProcessInstance

interface BpmnProcService {

    fun startProcess(processKey: String, variables: Map<String, Any?>): ProcessInstance

    fun getProcessInstance(processInstanceId: String) : ProcessInstance?

    fun getProcessDefinitionByInstanceId(processInstanceId: String) : ProcessDefinition?

    fun getProcessDefinition(processDefinitionId: String): ProcessDefinition?

}
