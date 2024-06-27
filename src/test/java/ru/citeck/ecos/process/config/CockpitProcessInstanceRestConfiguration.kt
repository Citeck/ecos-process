package ru.citeck.ecos.process.config

import org.camunda.bpm.cockpit.Cockpit
import org.camunda.bpm.cockpit.impl.DefaultCockpitRuntimeDelegate
import org.camunda.bpm.cockpit.impl.plugin.resources.ProcessInstanceRestService
import org.camunda.bpm.engine.ProcessEngine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CockpitProcessInstanceRestConfiguration(
    private val processEngine: ProcessEngine
) {

    @Bean
    fun camundaCockpitProcessInstanceRestService(): ProcessInstanceRestService {
        Cockpit.setCockpitRuntimeDelegate(DefaultCockpitRuntimeDelegate())
        return ProcessInstanceRestService(processEngine.name)
    }
}
