package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.rest

import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.rest.MigrationRestService
import org.camunda.bpm.engine.rest.impl.MigrationRestServiceImpl
import org.camunda.bpm.engine.rest.impl.ProcessInstanceRestServiceImpl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.camunda.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService as CockpitProcessInstanceRestService
import org.camunda.bpm.engine.rest.ProcessInstanceRestService as EngineProcessInstanceRestService

private const val DEFAULT_ENGINE_NAME = "default"

@Configuration
class CamundaRestServicesInjectConfiguration(
    private val processEngine: ProcessEngine
) {

    companion object {
        private val mapper = ObjectMapper()
    }

    @Bean
    @Profile("!test")
    fun camundaCockpitProcessInstanceRestService(): CockpitProcessInstanceRestService {
        return CockpitProcessInstanceRestService(processEngine.name)
    }

    @Bean
    fun camundaProcessInstanceRestService(): EngineProcessInstanceRestService {
        return ProcessInstanceRestServiceImpl(processEngine.name, mapper)
    }

    @Bean
    fun camundaMigrationRestService(): MigrationRestService {
        return MigrationRestServiceImpl(processEngine.name, mapper)
    }
}
