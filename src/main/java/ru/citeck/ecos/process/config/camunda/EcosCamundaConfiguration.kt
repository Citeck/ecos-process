package ru.citeck.ecos.process.config.camunda

import org.camunda.bpm.spring.boot.starter.configuration.CamundaDatasourceConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * @author Roman Makarskiy
 */
@Configuration
class EcosCamundaConfiguration {

    @Bean
    fun camundaDatasourceConfiguration(): CamundaDatasourceConfiguration {
        return CamundaCustomDataSourceConfiguration()
    }

}
