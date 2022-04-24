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

private const val ALFRESCO_APP = "alfresco"
private const val AUTHORITY_SRC_ID = "authority"
private const val PEOPLE_SRC_ID = "people"
private const val GROUP_PREFIX = "GROUP_"
private const val WORKSPACE_PREFIX = "workspace://"

@Slf4j
@Component("roles")
class CamundaRoleService(
    private val roleService: RoleService,
    private val recordsService: RecordsService
) : CamundaProcessEngineService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    private val isGroup = { name: String -> name.startsWith(GROUP_PREFIX) }
    private val isUser = { name: String -> !name.startsWith(GROUP_PREFIX) }

    override fun getKey(): String {
        return "roles"
    }

    fun getUserNames(document: String, roles: String): List<String> {
        return getRecipients(document, roles, isUser)
    }

    fun getGroupNames(document: String, roles: String): List<String> {
        return getRecipients(document, roles, isGroup)
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
        val recipientNames = roles.map {
            roleService.getAssignees(document, it)
        }.flatten().toSet()
        val recipientsFullFilledRefs = convertRecipientsToFullFilledRefs(recipientNames)

        val allUsers = mutableSetOf<RecordRef>()
        val groups = mutableListOf<RecordRef>()

        recipientsFullFilledRefs.forEach {
            if (it.isAuthorityGroupRef()) groups.add(it) else allUsers.add(it)
        }

        val usersFromGroup = recordsService.getAtts(groups, GroupInfo::class.java).map { it.containedUsers }.flatten()
        allUsers.addAll(usersFromGroup)

        return recordsService.getAtts(allUsers, UserInfo::class.java)
            .filter { it.email?.isNotBlank() ?: false }
            .map { it.email!! }
    }

    /**
     * Role service return userName or groupName. We need convert it to full recordRef format.
     * //TODO: migrate to model (microservice) people/groups after completion of development.
     */
    private fun convertRecipientsToFullFilledRefs(recipients: Collection<String>): List<RecordRef> {
        return recipients.map {
            if (it.startsWith(WORKSPACE_PREFIX)) {
                throw IllegalArgumentException("NodeRef format does not support. Recipient: $it")
            }

            var fullFilledRef = RecordRef.valueOf(it)

            if (fullFilledRef.appName.isBlank()) {
                fullFilledRef = fullFilledRef.addAppName(ALFRESCO_APP)
            }

            if (fullFilledRef.sourceId.isBlank()) {
                val sourceId = if (fullFilledRef.isAuthorityGroupRef()) AUTHORITY_SRC_ID else PEOPLE_SRC_ID
                fullFilledRef = fullFilledRef.withSourceId(sourceId)
            }

            fullFilledRef
        }
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

