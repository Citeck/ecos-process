package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.rest

import org.camunda.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val DEFAULT_ENGINE_NAME = "default"

@Configuration
class CamundaCockpitRestServicesInjectConfiguration {

    @Bean
    fun camundaCockpitProcessInstanceRestService(): ProcessInstanceRestService {
        return ProcessInstanceRestService(DEFAULT_ENGINE_NAME)
    }
}
