package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService as CockpitProcessInstanceRestService
import org.camunda.bpm.engine.rest.ProcessInstanceRestService as EngineProcessInstanceRestService
import org.camunda.bpm.engine.rest.impl.ProcessInstanceRestServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val DEFAULT_ENGINE_NAME = "default"

@Configuration
class CamundaRestServicesInjectConfiguration {

    @Bean
    fun camundaCockpitProcessInstanceRestService(): CockpitProcessInstanceRestService {
        return CockpitProcessInstanceRestService(DEFAULT_ENGINE_NAME)
    }

    @Bean
    fun camundaProcessInstanceRestService(): EngineProcessInstanceRestService {
        val objectMapper = ObjectMapper()
        return ProcessInstanceRestServiceImpl(DEFAULT_ENGINE_NAME, objectMapper)
    }
}
