package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services

import lombok.extern.slf4j.Slf4j
import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.isAuthorityGroupRef
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

const val GROUP_PREFIX = "GROUP_"

private const val WORKSPACE_PREFIX = "workspace://"

@Slf4j
@Component("roles")
class CamundaRoleService(
    private val roleService: RoleService,
    private val recordsService: RecordsService,
    private val authorityService: EcosAuthoritiesApi
) : CamundaProcessEngineService {

    companion object {
        private val log = KotlinLogging.logger {}

        const val KEY = "roles"
    }

    private val isGroup = { name: String -> name.startsWith(GROUP_PREFIX) }
    private val isUser = { name: String -> !name.startsWith(GROUP_PREFIX) }

    override fun getKey(): String {
        return KEY
    }

    fun getUserNames(document: String, roles: String): List<String> {
        return getRecipients(document, roles, isUser)
    }

    fun getGroupNames(document: String, roles: String): List<String> {
        return getRecipients(document, roles, isGroup)
    }

    fun getAuthorityNames(document: String, roles: String): List<String> {
        return getRecipients(document, roles) { true }
    }

    private fun getRecipients(document: String, roles: String, predicate: (String) -> Boolean): List<String> {
        val recipients = AuthContext.runAsSystem {
            val spRoles = roles.split(CAMUNDA_COLLECTION_SEPARATOR)
            if (spRoles.isEmpty()) emptyList<String>()

            spRoles.asSequence().map {
                roleService.getAssignees(RecordRef.valueOf(document), it.trim())
            }.flatten()
                .toSet()
                .filter { predicate.invoke(it) }
                .toList()
        }

        log.debug { "getRecipients for $document, roles: $roles. Return: $recipients" }

        return recipients
    }

    /**
     * @return emails of users, including users from groups
     */
    fun getEmails(document: RecordRef, roles: List<String>): List<String> {
        return AuthContext.runAsSystem {
            val recipientNames = roles.map {
                roleService.getAssignees(document, it)
            }.flatten().toSet()
            val recipientsFullFilledRefs = convertRecipientsToFullFilledRefs(recipientNames)

            val allUsers = mutableSetOf<EntityRef>()
            val groups = mutableListOf<EntityRef>()

            recipientsFullFilledRefs.forEach {
                if (it.isAuthorityGroupRef()) groups.add(it) else allUsers.add(it)
            }

            val usersFromGroup =
                recordsService.getAtts(groups, GroupInfo::class.java).map { it.containedUsers }.flatten()
            allUsers.addAll(usersFromGroup)

            val emails = recordsService.getAtts(allUsers, UserInfo::class.java)
                .filter { it.email?.isNotBlank() ?: false }
                .map { it.email!! }

            log.debug { "Get emails for document: $document, roles: $roles. Result: $emails" }

            emails
        }
    }

    private fun convertRecipientsToFullFilledRefs(recipients: Collection<String>): List<EntityRef> {
        recipients.map {
            if (it.startsWith(WORKSPACE_PREFIX)) {
                log.warn { "Convert nodeRef '$it' to authority refs. Maybe performance issue." }
            }
        }

        return authorityService.getAuthorityRefs(recipients.toList())
    }
}

private data class GroupInfo(
    @AttName("containedUsers")
    val containedUsers: List<RecordRef> = emptyList()
)

private data class UserInfo(
    @AttName("email")
    var email: String? = "",
)
