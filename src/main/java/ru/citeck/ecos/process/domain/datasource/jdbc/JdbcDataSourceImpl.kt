package ru.citeck.ecos.process.domain.datasource.jdbc

import javax.sql.DataSource

class JdbcDataSourceImpl(
    private val id: String,
    private val dataSource: DataSource
) : JdbcDataSource {

    override fun getId(): String = id

    override fun getJavaDataSource(): DataSource = dataSource
}
