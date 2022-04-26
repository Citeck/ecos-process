package ru.citeck.ecos.process.domain.datasource

interface EcosDataSource {

    fun getId(): String

    fun getType(): String
}
