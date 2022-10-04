package ru.citeck.ecos.process.domain.proctask.api.records

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.COMMENT_VAR
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.time.Instant

/**
 * @author Roman Makarskiy
 */
class ProcTaskRecord(
    val id: String,
    val priority: Int = 0,
    val formRef: RecordRef? = null,
    val processInstanceId: RecordRef? = null,
    val documentRef: RecordRef? = null,
    val title: MLText? = null,
    val created: Instant? = null,
    val ended: Instant? = null,
    val durationInMillis: Long? = null,
    val dueDate: Instant? = null,
    val actors: List<AuthorityDto> = emptyList(),

    val documentAtts: RecordAtts = RecordAtts(),
    val variables: Map<String, Any> = emptyMap(),

    val alfTaskAtts: RecordAtts = RecordAtts()
) {

    // TODO: add default form. simple-form is default?
    @get:AttName("_formRef")
    val formKey: RecordRef
        get() = formRef ?: RecordRef.create("uiserv", "form", "simple-form")

    @AttName("started")
    fun getStarted(): Instant? {
        return created
    }

    @AttName(".disp")
    fun getDisp(): MLText? {
        return title
    }

    @AttName("name")
    fun getName(): MLText? {
        return title
    }

    @AttName("workflow")
    fun getWorkflow(): RecordRef? {
        return processInstanceId
    }

    fun getAtt(name: String): Any? {
        val mapping = if (isAlfTask(id)) ProcTaskRecords.EPROC_TO_ALF_TASK_ATTS else ProcTaskRecords.ALF_TO_ERPOC_TASK_ATTS

        if (isAlfTask(id)) {
            if (mapping.containsKey(name)) {
                val fixedAttName = mapping[name]
                return alfTaskAtts.getAtt(fixedAttName)
            }

            return alfTaskAtts.getAtt(name)
        }

        if (name == COMMENT_VAR) {
            return null
        }

        if (mapping.containsKey(name)) {
            val fixedAttName = mapping[name]
            val attValue = when (fixedAttName) {
                "documentRef" -> documentRef
                "dueDate" -> dueDate
                "priority" -> priority
                "created" -> created
                "disp" -> title
                "id" -> id
                else -> null
            }

            if (attValue != null) {
                return attValue
            }
        }

        if (name.startsWith(DOCUMENT_FIELD_PREFIX)) {
            return documentAtts.getAtt(name.removePrefix(DOCUMENT_FIELD_PREFIX))
        }

        return variables[name]
    }
}

private fun isAlfTask(id: String): Boolean {
    return id.startsWith(ProcTaskRecords.ALF_TASK_PREFIX)
}
