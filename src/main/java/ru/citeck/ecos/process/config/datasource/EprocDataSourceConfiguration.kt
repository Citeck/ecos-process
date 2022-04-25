package ru.citeck.ecos.process.config.datasource

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("!test")
@Configuration
class EprocDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("ecos-process.datasource")
    fun eprocDataSourceProperties(): DataSourceProperties {
        return DataSourceProperties()
    }

    @Bean
    @ConfigurationProperties("ecos-process.datasource.hikari")
    fun eprocDataSource(
        @Qualifier("eprocDataSourceProperties") eprocDataSourceProperties: DataSourceProperties
    ): HikariDataSource {
        return eprocDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource::class.java).build()
    }
}
