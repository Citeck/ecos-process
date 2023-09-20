package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF

//TODO: write tests
@Component("businessKeyResolver")
class CamundaBusinessKeyResolver : CamundaProcessEngineService {
    override fun getKey(): String {
        return "businessKeyResolver"
    }

    fun resolve(execution: DelegateExecution): String? {
        return if (execution.businessKey.isNullOrBlank()) {
            return execution.getVariable(BPMN_DOCUMENT_REF) as? String
        } else {
            execution.businessKey
        }
    }
}
