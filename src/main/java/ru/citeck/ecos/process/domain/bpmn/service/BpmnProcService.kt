package ru.citeck.ecos.process.domain.bpmn.service

import org.camunda.bpm.engine.repository.ProcessDefinition
import org.camunda.bpm.engine.runtime.ProcessInstance

interface BpmnProcService {

    fun startProcess(processKey: String, businessKey: String? = null, variables: Map<String, Any?>): ProcessInstance

    fun setVariables(processInstanceId: String, variables: Map<String, Any?>)

    fun getProcessInstance(processInstanceId: String): ProcessInstance?

    fun getProcessInstancesForBusinessKey(businessKey: String): List<ProcessInstance>

    fun getProcessDefinitionByProcessInstanceId(processInstanceId: String): ProcessDefinition?

    fun getProcessDefinition(processDefinitionId: String): ProcessDefinition?

    fun getProcessDefinitionsByKey(processKey: String): List<ProcessDefinition>
}
