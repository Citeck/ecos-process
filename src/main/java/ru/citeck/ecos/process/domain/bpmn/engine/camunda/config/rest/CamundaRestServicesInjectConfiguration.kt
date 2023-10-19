package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.rest.MigrationRestService
import org.camunda.bpm.engine.rest.impl.MigrationRestServiceImpl
import org.camunda.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService as CockpitProcessInstanceRestService
import org.camunda.bpm.engine.rest.ProcessInstanceRestService as EngineProcessInstanceRestService
import org.camunda.bpm.engine.rest.impl.ProcessInstanceRestServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

private const val DEFAULT_ENGINE_NAME = "default"

@Configuration
class CamundaRestServicesInjectConfiguration {

    companion object {
        private val mapper = ObjectMapper()
    }

    @Bean
    fun camundaCockpitProcessInstanceRestService(): CockpitProcessInstanceRestService {
        return CockpitProcessInstanceRestService(DEFAULT_ENGINE_NAME)
    }

    @Bean
    fun camundaProcessInstanceRestService(): EngineProcessInstanceRestService {
        return ProcessInstanceRestServiceImpl(DEFAULT_ENGINE_NAME, mapper)
    }

    @Bean
    fun camundaMigrationRestService(): MigrationRestService {
        return MigrationRestServiceImpl(DEFAULT_ENGINE_NAME, mapper)
    }
}
