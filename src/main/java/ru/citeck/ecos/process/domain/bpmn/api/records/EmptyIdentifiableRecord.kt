package ru.citeck.ecos.process.domain.bpmn.api.records

class EmptyIdentifiableRecord(
    var id: String
) : IdentifiableRecord {
    override fun getIdentificator(): String {
        return id
    }
}
