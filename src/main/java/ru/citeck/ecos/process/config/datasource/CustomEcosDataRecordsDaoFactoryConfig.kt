package ru.citeck.ecos.process.config.datasource

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.citeck.ecos.data.sql.datasource.DbDataSource
import ru.citeck.ecos.data.sql.pg.spring.EcosDataRecordsDaoFactoryConfig
import javax.sql.DataSource

@Configuration
@Profile("!test")
class CustomEcosDataRecordsDaoFactoryConfig : EcosDataRecordsDaoFactoryConfig() {

    @Bean
    override fun dbDataSource(@Qualifier("eprocDataSource") dataSource: DataSource): DbDataSource {
        return super.dbDataSource(dataSource)
    }
}
