package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_CAMUNDA_COLLECTION_SEPARATOR
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.isAuthorityGroupRef
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.regex.Pattern

private const val WORKSPACE_PREFIX = "workspace://"

@Component("roles")
class CamundaRoleService(
    private val roleService: RoleService,
    private val mailUtils: MailUtils
) : CamundaProcessEngineService {

    companion object {
        private val log = KotlinLogging.logger {}

        const val KEY = "roles"
    }

    private val isGroup = { name: String -> name.startsWith(AuthGroup.PREFIX) }
    private val isUser = { name: String -> !name.startsWith(AuthGroup.PREFIX) }

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
            val spRoles = roles.split(BPMN_CAMUNDA_COLLECTION_SEPARATOR)
            if (spRoles.isEmpty()) emptyList<String>()

            spRoles.asSequence().map {
                roleService.getAssignees(EntityRef.valueOf(document), it.trim())
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
    fun getEmails(document: EntityRef, roles: List<String>): List<String> {
        return AuthContext.runAsSystem {
            val recipientNames = roles.map {
                roleService.getAssignees(document, it)
            }.flatten()

            val result = mailUtils.getEmails(recipientNames).toList()

            log.debug { "Get emails for document: $document, roles: $roles. Result: $result" }

            result
        }
    }
}

@Component
class MailUtils(
    private val authorityService: EcosAuthoritiesApi,
    private val recordsService: RecordsService,
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val emailPattern = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\$")

    /**
     * [data] list, where element can be:
     * - group name or ref
     * - username or ref
     * - nodeRef
     * - email
     *
     * @return unique emails received from [data]
     */
    fun getEmails(data: List<String>): Set<String> {
        return AuthContext.runAsSystem {
            val recipients = mutableListOf<String>()
            val incomeEmails = mutableListOf<String>()

            data.forEach {
                if (emailPattern.matcher(it).matches()) {
                    incomeEmails.add(it)
                } else {
                    recipients.add(it)
                }
            }

            val fullFilledRefs = convertRecipientsToFullFilledRefs(recipients.toSet())

            val allUsers = mutableSetOf<EntityRef>()
            val groups = mutableListOf<EntityRef>()

            fullFilledRefs.forEach {
                if (it.isAuthorityGroupRef()) {
                    groups.add(it)
                } else {
                    allUsers.add(it)
                }
            }

            val usersFromGroup = recordsService.getAtts(groups, GroupInfo::class.java)
                .map { it.containedUsers }
                .flatten()
            allUsers.addAll(usersFromGroup)

            val emails = recordsService.getAtts(allUsers, UserInfo::class.java)
                .filter { it.email?.isNotBlank() ?: false }
                .map { it.email!! }

            (incomeEmails + emails).toSet()
        }
    }

    fun getUserTimeZoneByEmail(email: String): String? {
        return AuthContext.runAsSystem {
            recordsService.queryOne(
                RecordsQuery.create()
                    .withSourceId("emodel/person")
                    .withQuery(Predicates.eq("email", email))
                    .withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    .build(),
                "timezone"
            ).asText("")
        }
    }

    private fun convertRecipientsToFullFilledRefs(recipients: Collection<String>): Set<EntityRef> {
        val authorityRefs = mutableSetOf<EntityRef>()

        val authoritiesToResolveRefs = mutableListOf<String>()

        recipients.forEach { recipient ->
            if (isNodeRefOrAuthorityName(recipient)) {
                authoritiesToResolveRefs.add(recipient)
            } else {
                authorityRefs.add(EntityRef.valueOf(recipient))
            }
        }

        val resolvedRefs = authorityService.getAuthorityRefs(recipients.toList())

        return authorityRefs + resolvedRefs
    }

    private fun isNodeRefOrAuthorityName(recipient: String): Boolean {
        if (recipient.startsWith(WORKSPACE_PREFIX)) {
            log.warn { "Convert nodeRef '$recipient' to authority refs. Maybe performance issue." }
            return true
        }

        return EntityRef.valueOf(recipient).getSourceId().isBlank()
    }
}

private data class GroupInfo(
    @AttName("containedUsers")
    val containedUsers: List<EntityRef> = emptyList()
)

private data class UserInfo(
    @AttName("email")
    var email: String? = ""
)
