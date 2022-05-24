package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.util.*

data class ProcTaskRecord(
    val id: String? = null,
    val formRef: RecordRef? = null,
    val title: String? = null,
    val created: Date? = null,
    val dueDate: Date? = null,
    val actors: List<AuthorityDto> = emptyList(),
    val documentAtts: RecordAtts = RecordAtts()
) {

    // TODO: add default form. simple-form is default?
    @get:AttName("_formRef")
    val formKey: RecordRef
        get() = formRef ?: RecordRef.create("uiserv", "form", "simple-form")

    @AttName("started")
    fun getStarted(): Date? {
        return created
    }

    fun getAtt(name: String): Any? {
        if (name.startsWith(DOCUMENT_FIELD_PREFIX)) {
            return documentAtts.getAtt(name.removePrefix(DOCUMENT_FIELD_PREFIX))
        }

        return null
    }
}
