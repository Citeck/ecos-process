package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.datasource.CamundaCustomDataSourceConfiguration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.config.CamundaSystemContextConfiguration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.config.CamundaResolveVariablesConfiguration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.config.CamundaScriptEnvResolvesConfiguration

/**
 * @author Roman Makarskiy
 */
@Configuration
class EcosCamundaConfiguration {

    @Bean
    fun camundaDatasourceConfiguration(): CamundaDatasourceConfiguration {
        return CamundaCustomDataSourceConfiguration()
    }

    @Bean
    fun camundaServicesConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaServicesConfiguration()
    }

    @Bean
    fun camundaResolveVariablesConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaResolveVariablesConfiguration()
    }

    @Bean
    fun camundaSystemContextConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaSystemContextConfiguration()
    }

    @Bean
    fun camundaScriptEnvResolvesConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaScriptEnvResolvesConfiguration()
    }
}