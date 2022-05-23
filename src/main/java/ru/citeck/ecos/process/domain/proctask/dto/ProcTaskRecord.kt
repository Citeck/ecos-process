package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import java.util.*

data class ProcTaskRecord(
    val id: String? = null,
    val formRef: RecordRef? = null,
    val title: String? = null,
    val created: Date? = null,
    val dueDate: Date? = null,
    val actors: List<AuthorityDto> = emptyList()
) {

    // TODO: add default form. simple-form is default?
    @get:AttName("_formRef")
    val formKey: RecordRef
        get() = formRef ?: RecordRef.create("uiserv", "form", "simple-form")

    @AttName("started")
    fun getStarted(): Date? {
        return created
    }

    //TODO: get ECM fields?
    /*override fun getAtt(name: String): Any? {
        println("Get att: $name")
        return super.getAtt(name)
    }*/
}
