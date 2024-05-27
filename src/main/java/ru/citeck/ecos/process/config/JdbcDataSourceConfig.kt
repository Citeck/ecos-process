package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.datasource.JdbcDataSource
import ru.citeck.ecos.webapp.lib.spring.context.datasource.EcosDataSourceManager

@Configuration
class JdbcDataSourceConfig {

    // Used by ECOS Data
    @Bean
    @Primary
    fun jdbcDataSource(ecosDataSourceManager: EcosDataSourceManager): JdbcDataSource {
        return ecosDataSourceManager.getDataSource(AppName.EPROC, JdbcDataSource::class.java)
    }
}
