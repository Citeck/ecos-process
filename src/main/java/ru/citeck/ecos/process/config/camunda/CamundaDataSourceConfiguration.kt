package ru.citeck.ecos.process.config.camunda

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * @author Roman Makarskiy
 */
@Configuration
class CamundaDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("ecos-process.camunda.datasource")
    fun camundaDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean
    @ConfigurationProperties("ecos-process.camunda.datasource.hikari")
    fun camundaDataSource(
        @Qualifier("camundaDataSourceProperties") camundaDataSourceProperties: DataSourceProperties
    ): HikariDataSource {
        return camundaDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
    }

    @Bean
    fun camundaTransactionManager(@Qualifier("camundaDataSource") dataSource: DataSource): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource)
    }

}
