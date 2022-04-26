package ru.citeck.ecos.process.domain.datasource.jdbc

import ru.citeck.ecos.process.domain.datasource.EcosDataSource
import javax.sql.DataSource

interface JdbcDataSource : EcosDataSource {

    companion object {
        const val TYPE = "jdbc"
    }

    fun getJavaDataSource(): DataSource

    override fun getType() = TYPE
}
