package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.rabbitmq.ds.RabbitMqConnection
import ru.citeck.ecos.rabbitmq.ds.RabbitMqDataSource
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class BpmnRabbitmqConnectionConfig {

    @Bean
    fun bpmnRabbitmqConnection(ecosDataSourceManager: EcosDataSourceManager): RabbitMqConnection {
        return ecosDataSourceManager.getDataSource("bpmn-rabbitmq", RabbitMqDataSource::class.java).getConnection()
    }
}
