package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.springframework.stereotype.Component

@Component("bpmnTestBean")
class BpmnTestBean : CamundaProcessEngineService {

    override fun getKey(): String {
        return "bpmnTestBean"
    }

    fun returnValue(value: String): String {
        return value
    }

}
