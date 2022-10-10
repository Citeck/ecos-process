package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.COMMENT_VAR
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import javax.annotation.PostConstruct

internal val log = KotlinLogging.logger {}

/**
 * @author Roman Makarskiy
 */
@Component
class ProcTaskRecordServiceProvider(
    val recordsService: RecordsService,
    val procTaskService: ProcTaskService
) {

    @PostConstruct
    private fun init() {
        prv = this
    }
}

private lateinit var prv: ProcTaskRecordServiceProvider

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

    val assignee: EntityRef = RecordRef.EMPTY,
    val candidateUsers: List<EntityRef> = emptyList(),
    val candidateGroups: List<EntityRef> = emptyList(),

    val documentAtts: RecordAtts = RecordAtts(),

    val alfTaskAtts: RecordAtts = RecordAtts(),

    val engineAtts: List<String> = emptyList(),

    val historic: Boolean = false
) {

    // TODO: add default form. simple-form is default?
    @get:AttName("_formRef")
    val formKey: RecordRef
        get() = formRef ?: RecordRef.create("uiserv", "form", "simple-form")

    @get:AttName(RecordConstants.ATT_CREATED)
    val recordCreated: Instant?
        get() = created

    @AttName("started")
    fun getStarted(): Instant? {
        return created
    }

    @AttName("actors")
    fun getActors(): List<AuthorityDto> {
        val candidates = candidateUsers + candidateGroups

        val actorsRefs = if (EntityRef.isNotEmpty(assignee)) {
            listOf(assignee)
        } else {
            candidates
        }
        return prv.recordsService.getAtts(actorsRefs, AuthorityDto::class.java)
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
        val mapping =
            if (isAlfTask(id)) ProcTaskRecords.EPROC_TO_ALF_TASK_ATTS else ProcTaskRecords.ALF_TO_ERPOC_TASK_ATTS

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

        if (!historic && engineAtts.contains(name)) {
            val variables = prv.procTaskService.getVariables(id)
            val value = variables[name]

            log.debug { "procTaskRecord $id request to task engine variables $id = $name. Possible performance issues" }

            return value
        }

        return null
    }
}

private fun isAlfTask(id: String): Boolean {
    return id.startsWith(ProcTaskRecords.ALF_TASK_PREFIX)
}
