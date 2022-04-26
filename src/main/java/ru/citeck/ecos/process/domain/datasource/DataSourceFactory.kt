package ru.citeck.ecos.process.domain.datasource

import ru.citeck.ecos.process.domain.datasource.jdbc.JdbcDataSource

interface DataSourceFactory {

    /**
     * @throws ru.citeck.ecos.process.domain.datasource.exception.DataSourceNotFound
     */
    fun getJdbcDataSource(id: String): JdbcDataSource {
        return getDataSource(id, JdbcDataSource.TYPE)
    }

    /**
     * @throws ru.citeck.ecos.process.domain.datasource.exception.DataSourceNotFound
     * @throws ru.citeck.ecos.process.domain.datasource.exception.UnsupportedDataSourceType
     */
    fun <T : EcosDataSource> getDataSource(id: String, type: String): T
}
