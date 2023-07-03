package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.camunda.bpm.engine.delegate.BpmnError
import org.springframework.stereotype.Component

@Component("bpmnTestBean")
class BpmnTestBean : CamundaProcessEngineService {

    override fun getKey(): String {
        return "bpmnTestBean"
    }

    fun returnValue(value: String): String {
        return value
    }

    fun throwBpmnError(errorCode: String) {
        throw BpmnError(errorCode)
    }

    fun throwBpmnError(errorCode: String, message: String) {
        throw BpmnError(errorCode, message)
    }

}
