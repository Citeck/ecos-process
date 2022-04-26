package ru.citeck.ecos.process.domain.datasource

interface DataSourceTypeFactory<P : Any, T : EcosDataSource> {

    fun create(id: String, props: P): T

    fun getType(): String
}
