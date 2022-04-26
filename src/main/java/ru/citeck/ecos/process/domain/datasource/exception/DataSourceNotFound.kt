package ru.citeck.ecos.process.domain.datasource.exception

class DataSourceNotFound(val id: String, val type: String) : RuntimeException("id: '$id' type: '$type'")
