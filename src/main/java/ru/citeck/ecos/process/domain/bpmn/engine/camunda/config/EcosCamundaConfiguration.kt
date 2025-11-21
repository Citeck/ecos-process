package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration
import org.camunda.bpm.spring.boot.starter.configuration.CamundaProcessEngineConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.datasource.CamundaCustomDataSourceConfiguration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.system.config.CamundaSystemContextConfiguration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.config.CamundaResolveVariablesConfiguration
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.config.CamundaScriptEnvResolvesConfiguration
import ru.citeck.ecos.process.domain.bpmn.utils.ProcUtils
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProps
import ru.citeck.ecos.webapp.lib.env.EcosWebAppEnvironment
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

/**
 * @author Roman Makarskiy
 */
@Configuration
@DependsOn("liquibaseSchemaManagementProvider")
class EcosCamundaConfiguration(
    private val ecosDataSourceManager: EcosDataSourceManager
) {

    @Bean
    fun camundaDataSource(): JdbcDataSource {
        return ecosDataSourceManager.getDataSource("camunda", JdbcDataSource::class.java)
    }

    @Bean
    fun camundaDatasourceConfiguration(): CamundaDatasourceConfiguration {
        return CamundaCustomDataSourceConfiguration()
    }

    @Bean
    fun camundaServicesConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaServicesConfiguration()
    }

    @Bean
    fun camundaSerializationConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaSerializationConfiguration()
    }

    @Bean
    fun camundaResolveVariablesConfiguration(
        ecosWebAppProps: EcosWebAppProps
    ): CamundaProcessEngineConfiguration {
        return CamundaResolveVariablesConfiguration(ecosWebAppProps)
    }

    @Bean
    fun camundaSystemContextConfiguration(procUtils: ProcUtils): CamundaProcessEngineConfiguration {
        return CamundaSystemContextConfiguration(procUtils)
    }

    @Bean
    fun camundaScriptEnvResolvesConfiguration(): CamundaProcessEngineConfiguration {
        return CamundaScriptEnvResolvesConfiguration()
    }

    @Bean
    fun ecosCamundaEnginePlugin(ecosWebAppEnvironment: EcosWebAppEnvironment): EcosCamundaEnginePlugin {
        return EcosCamundaEnginePlugin(ecosWebAppEnvironment)
    }
}
