package ru.citeck.ecos.process.domain.datasource.exception

class UnsupportedDataSourceType(val type: String) : RuntimeException("Type: $type")
