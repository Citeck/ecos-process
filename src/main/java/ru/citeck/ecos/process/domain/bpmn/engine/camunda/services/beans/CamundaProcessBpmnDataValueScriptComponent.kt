package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue

@Component("DataValue")
class CamundaProcessBpmnDataValueScriptComponent : CamundaProcessEngineService {
    override fun getKey(): String {
        return "DataValue"
    }

    fun of(content: Any?): BpmnDataValue {
        return BpmnDataValue.create(content)
    }

    fun createObj(): BpmnDataValue {
        return BpmnDataValue.createObj()
    }

    fun createArr(): BpmnDataValue {
        return BpmnDataValue.createArr()
    }

    fun createStr(value: Any?): BpmnDataValue {
        return BpmnDataValue.createStr(value)
    }
}
