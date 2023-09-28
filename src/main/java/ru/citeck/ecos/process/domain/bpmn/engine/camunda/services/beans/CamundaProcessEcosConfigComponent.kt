package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.config.lib.service.EcosConfigService

@Component("Config")
class CamundaProcessEcosConfigComponent(
    private val ecosConfigService: EcosConfigService
) : CamundaProcessEngineService {

    override fun getKey(): String {
        return "Config"
    }

    fun get(key: String): BpmnDataValue {
        return BpmnDataValue.create(ecosConfigService.getValue(key))
    }

    fun getOrDefault(key: String, defaultValue: Any): BpmnDataValue {
        val result = BpmnDataValue.create(ecosConfigService.getValue(key))
        if (result.isNotNull()) {
            return result
        }
        return BpmnDataValue.create(defaultValue)
    }

    fun getNotNull(key: String): BpmnDataValue {
        val result = BpmnDataValue.create(ecosConfigService.getValue(key))
        if (result.isNull()) {
            error("Config value for key '$key' is null")
        }
        return result
    }
}
