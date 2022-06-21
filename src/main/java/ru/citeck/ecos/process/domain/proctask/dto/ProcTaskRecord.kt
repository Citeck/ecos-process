package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.util.*

data class ProcTaskRecord(
    val id: String? = null,
    val priority: Int = 0,
    val formRef: RecordRef? = null,
    val processInstanceId: RecordRef? = null,
    val documentRef: RecordRef? = null,
    val title: String? = null,
    val created: Date? = null,
    val dueDate: Date? = null,
    val actors: List<AuthorityDto> = emptyList(),
    val documentAtts: RecordAtts = RecordAtts(),
    val variables: Map<String, Any> = emptyMap()
) {

    // TODO: add default form. simple-form is default?
    @get:AttName("_formRef")
    val formKey: RecordRef
        get() = formRef ?: RecordRef.create("uiserv", "form", "simple-form")

    @AttName("started")
    fun getStarted(): Date? {
        return created
    }

    @AttName(".disp")
    fun getDisp(): String? {
        return title
    }

    @AttName("name")
    fun getName(): String? {
        return title
    }

    @AttName("workflow")
    fun getWorkflow(): RecordRef? {
        return processInstanceId
    }

    fun getAtt(name: String): Any? {
        if (name.startsWith(DOCUMENT_FIELD_PREFIX)) {
            return documentAtts.getAtt(name.removePrefix(DOCUMENT_FIELD_PREFIX))
        }

        return variables[name]
    }
}
