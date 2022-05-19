package ru.citeck.ecos.process.config.camunda

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

/**
 * @author Roman Makarskiy
 */
@Configuration
class CamundaDataSourceConfiguration {

    @Bean
    fun camundaTransactionManager(dsManager: EcosDataSourceManager): PlatformTransactionManager {
        return DataSourceTransactionManager(
            dsManager.getDataSource(
                "camunda", JdbcDataSource::class.java
            ).getJavaDataSource()
        )
    }
}
