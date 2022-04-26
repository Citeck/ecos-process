package ru.citeck.ecos.process.config.camunda

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import ru.citeck.ecos.process.domain.datasource.DataSourceFactory

/**
 * @author Roman Makarskiy
 */
@Configuration
class CamundaDataSourceConfiguration {

    @Bean
    fun camundaTransactionManager(dataSource: DataSourceFactory): PlatformTransactionManager {
        return DataSourceTransactionManager(dataSource.getJdbcDataSource("camunda").getJavaDataSource())
    }

}
