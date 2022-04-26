package ru.citeck.ecos.process.config.datasource

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.citeck.ecos.process.domain.datasource.DataSourceFactory
import javax.sql.DataSource

@Configuration
@Profile("!test")
class EprocDataSourceConfiguration {

    @Bean
    fun eprocDataSource(dataSourceFactory: DataSourceFactory): DataSource {
        return dataSourceFactory.getJdbcDataSource("eproc").getJavaDataSource()
    }
}
