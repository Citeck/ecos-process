package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.datasource

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.lib.spring.context.txn.EcosTxnToSpringTxnManagerAdapter

/**
 * @author Roman Makarskiy
 */
@Configuration
class CamundaDataSourceConfiguration {

    @Bean
    fun camundaTransactionManager(
        @Qualifier("camundaDataSource")
        camundaDataSource: JdbcDataSource
    ): PlatformTransactionManager {
        return if (camundaDataSource.isManaged()) {
            EcosTxnToSpringTxnManagerAdapter()
        } else {
            DataSourceTransactionManager(camundaDataSource.getJavaDataSource())
        }
    }
}
