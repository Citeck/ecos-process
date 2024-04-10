package ru.citeck.ecos.process.domain.proctask.api.records

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.DOCUMENT_FIELD_PREFIX
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.proctask.dto.AuthorityDto
import ru.citeck.ecos.process.domain.proctask.service.ProcTaskService
import ru.citeck.ecos.process.domain.proctask.service.TASK_OWNERSHIP_REASSIGN_ALLOWED_GROUP
import ru.citeck.ecos.process.domain.proctask.service.isCurrentUserTaskActorOrDelegate
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import javax.annotation.PostConstruct

internal val log = KotlinLogging.logger {}

private const val TASK_PERMISSION_REASSIGN = "Reassign"
private const val ATT_PREVIEW_INFO_JSON = "previewInfo${ScalarType.JSON_SCHEMA}"

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
    val formRef: RecordRef = RecordRef.EMPTY,
    val processInstanceRef: RecordRef? = null,
    val documentRef: RecordRef? = null,
    val documentType: String? = null,
    val documentTypeRef: RecordRef? = null,
    val title: MLText? = null,
    val created: Instant? = null,
    val ended: Instant? = null,
    val durationInMillis: Long? = null,
    val dueDate: Instant? = null,
    val followUpDate: Instant? = null,
    val definitionKey: String? = null,

    val owner: EntityRef = RecordRef.EMPTY,
    val assignee: EntityRef = RecordRef.EMPTY,

    val senderTemp: EntityRef = RecordRef.EMPTY,

    val candidateUsers: List<EntityRef> = emptyList(),
    val candidateGroups: List<EntityRef> = emptyList(),

    val possibleOutcomes: List<TaskOutcome> = emptyList(),

    val comment: String? = null,
    val lastComment: String? = null,

    val documentAtts: RecordAtts = RecordAtts(),

    val alfTaskAtts: RecordAtts = RecordAtts(),

    val engineAtts: List<String> = emptyList(),

    val historic: Boolean = false,

    val reassignable: Boolean = false,
    val claimable: Boolean = false,
    val unclaimable: Boolean = false,
    val assignable: Boolean = false,
) {

    val permissions = object : AttValue {

        override fun has(name: String): Boolean {
            if (AuthContext.isRunAsAdmin() || AuthContext.isRunAsSystem()) {
                return true
            }

            if (this@ProcTaskRecord.isCurrentUserTaskActorOrDelegate()) {
                return true
            }

            if (TASK_PERMISSION_REASSIGN.equals(name, ignoreCase = true)) {
                return AuthContext.getCurrentAuthorities().contains(TASK_OWNERSHIP_REASSIGN_ALLOWED_GROUP)
            }

            log.warn { "Request unknown permission <$name> for task ${this@ProcTaskRecord.id}" }
            return false
        }
    }

    @get:AttName("_formRef")
    val formKey: RecordRef
        get() = if (formRef.isNotEmpty()) {
            formRef
        } else {
            RecordRef.create(AppName.UISERV, "form", "proc_task_default_form")
        }

    @get:AttName(RecordConstants.ATT_CREATED)
    val recordCreated: Instant?
        get() = created

    @AttName("started")
    fun getStarted(): Instant? {
        return created
    }

    @AttName("releasable")
    fun getReleasable(): Boolean {
        return unclaimable
    }

    // TODO: refactor task widget to using new api (request sender entityRef). Remove with method below.
    // Rename senderTemp to sender
    @AttName("sender")
    fun getSender(): AuthorityDto {
        return prv.recordsService.getAtts(senderTemp, AuthorityDto::class.java)
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
        return processInstanceRef
    }

    fun getPreviewInfo(): DataValue {
        return prv.recordsService.getAtt(documentRef, ATT_PREVIEW_INFO_JSON)
    }

    @AttName("lastcomment")
    fun getLastCommentLegacyAtt(): String? {
        return lastComment
    }

    fun getAtt(name: String): Any? {
        val mapping = if (isAlfTask(id)) {
            ProcTaskRecords.EPROC_TO_ALF_TASK_ATTS
        } else {
            ProcTaskRecords.ALF_TO_ERPOC_TASK_ATTS
        }

        if (isAlfTask(id)) {
            if (mapping.containsKey(name)) {
                val fixedAttName = mapping[name]
                return alfTaskAtts.getAtt(fixedAttName)
            }

            return alfTaskAtts.getAtt(name)
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
            val value = prv.procTaskService.getVariable(id, name)

            log.debug { "procTaskRecord $id request to task engine variables $id = $name. Possible performance issues" }

            return value
        }

        return null
    }
}

private fun isAlfTask(id: String): Boolean {
    return id.startsWith(ProcTaskRecords.ALF_TASK_PREFIX)
}
