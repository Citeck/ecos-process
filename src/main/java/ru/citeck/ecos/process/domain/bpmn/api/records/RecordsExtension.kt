package ru.citeck.ecos.process.domain.bpmn.api.records

interface IdentifiableRecord {

    fun getIdentificator(): String
}

fun <T : IdentifiableRecord> List<T>.sortByIds(ids: List<String>): List<T> {
    val map = associateBy { it.getIdentificator() }
    return ids.mapNotNull { map[it] }
}
