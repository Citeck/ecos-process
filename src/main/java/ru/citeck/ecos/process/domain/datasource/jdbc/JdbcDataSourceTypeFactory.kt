package ru.citeck.ecos.process.domain.datasource.jdbc

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.datasource.DataSourceTypeFactory

@Component
@Profile("!test")
class JdbcDataSourceTypeFactory : DataSourceTypeFactory<JdbcConnectionProperties, JdbcDataSource> {

    override fun create(id: String, props: JdbcConnectionProperties): JdbcDataSource {

        val dsProps = DataSourceProperties()
        dsProps.url = props.url
        dsProps.username = props.username
        dsProps.password = props.password
        dsProps.type = HikariDataSource::class.java
        if (props.driverClassName.isNotBlank()) {
            dsProps.driverClassName = props.driverClassName
        }

        val dataSource: HikariDataSource = dsProps.initializeDataSourceBuilder().build() as HikariDataSource
        dataSource.isAutoCommit = false
        dataSource.poolName = "$id-pool"

        return JdbcDataSourceImpl(id, dataSource)
    }

    override fun getType(): String = JdbcDataSource.TYPE
}
