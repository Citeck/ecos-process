package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
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
    private val mailUtils: MailUtils,
    private val recordsService: RecordsService
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
        if (roles.isEmpty()) {
            return emptyList()
        }
        return AuthContext.runAsSystem {
            val assignees = roleService.getAssignees(document, roles).values.flatten()
            val result = mailUtils.getEmails(assignees).toList()
            log.debug { "Get emails for document: $document, roles: $roles. Result: $result" }
            result
        }
    }

    /**
     * @return emails per role separately (preserves the role -> emails grouping).
     */
    fun getEmailsByRole(document: EntityRef, roles: List<String>): Map<String, List<String>> {
        if (roles.isEmpty()) {
            return emptyMap()
        }
        return AuthContext.runAsSystem {
            val assigneesByRole = roleService.getAssignees(document, roles)
            val emailsByAuthority = mailUtils.getEmailsByAuthority(
                assigneesByRole.values.flatten().toSet()
            )
            val result = roles.associateWith { roleId ->
                assigneesByRole[roleId].orEmpty()
                    .flatMap { emailsByAuthority[it].orEmpty() }
                    .distinct()
            }
            log.debug { "Get emails by role for document: $document, roles: $roles. Result: $result" }
            result
        }
    }

    /**
     * @return the role display name ([RoleDef.name]), or empty MLText if the role/type is not found.
     */
    fun getRoleName(document: EntityRef, roleId: String): MLText {
        if (roleId.isBlank()) {
            return MLText.EMPTY
        }
        return getRoleNames(document, listOf(roleId))[roleId] ?: MLText.EMPTY
    }

    /**
     * Batch variant of [getRoleName]: resolves the document type only once for the whole [roleIds]
     * set instead of re-fetching `_type?id` per role.
     *
     * @return role id -> display name ([RoleDef.name]); empty MLText for a role whose def or the
     *   document type is not found.
     */
    fun getRoleNames(document: EntityRef, roleIds: List<String>): Map<String, MLText> {
        if (roleIds.isEmpty()) {
            return emptyMap()
        }
        return AuthContext.runAsSystem {
            val typeId = recordsService.getAtt(document, "_type?id").asText()
            if (typeId.isBlank()) {
                return@runAsSystem roleIds.associateWith { MLText.EMPTY }
            }
            val typeRef = EntityRef.valueOf(typeId)
            roleIds.associateWith { roleId ->
                roleService.getRoleDef(typeRef, roleId).name
            }
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
    fun getEmails(data: Collection<String>): Set<String> {
        return getEmailsByAuthority(data).values.flatten().toSet()
    }

    /**
     * Same resolution as [getEmails] (literal emails pass through, groups are expanded to their
     * users, disabled users and users without email are dropped), but the association between each
     * input authority and its emails is preserved.
     *
     * @return map: input value -> emails resolved from it
     */
    fun getEmailsByAuthority(data: Collection<String>): Map<String, List<String>> {
        return AuthContext.runAsSystem {
            val distinctData = data.distinct()
            val result = LinkedHashMap<String, MutableList<String>>()
            distinctData.forEach { result[it] = mutableListOf() }

            val recipients = mutableListOf<String>()

            distinctData.forEach {
                if (emailPattern.matcher(it).matches()) {
                    result.getValue(it).add(it)
                } else {
                    recipients.add(it)
                }
            }

            if (recipients.isNotEmpty()) {
                // Each recipient resolves to a *set* of candidate refs: its locally parsed ref and
                // the ref returned by the batched authority lookup. Both are queried below - for a
                // canonical input they coincide (no extra query cost), but for an input that looks
                // like a ref without being a canonical emodel authority ref (e.g. an alfresco-app
                // ref, or an app-less "person@ivan"), the locally parsed ref is the one that
                // actually answers `email`/`personDisabled`, and would otherwise be silently
                // dropped.
                val refsByRecipient = convertRecipientsToFullFilledRefsIndexed(recipients)

                val groupRefsByRecipient = LinkedHashMap<String, List<EntityRef>>()
                val userRefsByRecipient = LinkedHashMap<String, List<EntityRef>>()

                refsByRecipient.forEach { (recipient, refs) ->
                    val (groupRefs, userRefs) = refs.partition { it.isAuthorityGroupRef() }
                    if (groupRefs.isNotEmpty()) {
                        groupRefsByRecipient[recipient] = groupRefs
                    }
                    if (userRefs.isNotEmpty()) {
                        userRefsByRecipient[recipient] = userRefs
                    }
                }

                val allGroupRefs = groupRefsByRecipient.values.flatten().distinct()
                val containedUsersByGroupRef = if (allGroupRefs.isNotEmpty()) {
                    val groupsInfo = recordsService.getAtts(allGroupRefs, GroupInfo::class.java)
                    allGroupRefs.mapIndexed { idx, ref -> ref to groupsInfo[idx].containedUsers }.toMap()
                } else {
                    emptyMap()
                }

                val usersByGroupRecipient = LinkedHashMap<String, List<EntityRef>>()
                val userRefsToResolve = LinkedHashSet<EntityRef>()
                userRefsByRecipient.values.forEach { userRefsToResolve.addAll(it) }

                groupRefsByRecipient.forEach { (recipient, groupRefs) ->
                    val members = groupRefs.flatMap { containedUsersByGroupRef[it].orEmpty() }.distinct()
                    usersByGroupRecipient[recipient] = members
                    userRefsToResolve.addAll(members)
                }

                val userRefList = userRefsToResolve.toList()
                val emailByUserRef = if (userRefList.isNotEmpty()) {
                    recordsService.getAtts(userRefList, UserInfo::class.java)
                        .mapIndexed { idx, info -> userRefList[idx] to info }
                        .filter { (_, info) -> info.disabled != true && !info.email.isNullOrBlank() }
                        .associate { (ref, info) -> ref to info.email!! }
                } else {
                    emptyMap()
                }

                userRefsByRecipient.forEach { (recipient, refs) ->
                    refs.forEach { ref -> emailByUserRef[ref]?.let { result.getValue(recipient).add(it) } }
                }

                usersByGroupRecipient.forEach { (recipient, members) ->
                    members.forEach { memberRef ->
                        emailByUserRef[memberRef]?.let { result.getValue(recipient).add(it) }
                    }
                }
            }

            result.mapValues { it.value.distinct() }
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

    /**
     * Resolves [recipients] to their full-filled [EntityRef] candidates, preserving the input ->
     * refs association. Uses a single batched, index-aligned [EcosAuthoritiesApi.getAuthorityRefs]
     * call over the WHOLE input collection, and unions each result with the recipient's locally
     * parsed [EntityRef] (deduplicated) - restoring the old union semantics of
     * `convertRecipientsToFullFilledRefs` (which passed the whole collection to
     * `getAuthorityRefs` and returned `authorityRefs + resolvedRefs`) without adding queries: for a
     * canonical input both refs coincide, but for an input that looks like a ref without being a
     * canonical emodel authority ref (e.g. `alfresco/people@admin`, or an app-less `person@ivan`),
     * only the locally parsed ref answers `email`/`personDisabled`, and narrowing to just the
     * resolved ref would silently drop that recipient's email.
     */
    private fun convertRecipientsToFullFilledRefsIndexed(recipients: List<String>): Map<String, Set<EntityRef>> {
        if (recipients.isEmpty()) {
            return emptyMap()
        }

        recipients.forEach { recipient ->
            if (recipient.startsWith(WORKSPACE_PREFIX)) {
                log.warn { "Convert nodeRef '$recipient' to authority refs. Maybe performance issue." }
            }
        }

        val resolvedRefs = authorityService.getAuthorityRefs(recipients)

        val result = LinkedHashMap<String, Set<EntityRef>>()
        recipients.forEachIndexed { idx, recipient ->
            val localRef = EntityRef.valueOf(recipient)
            val resolvedRef = resolvedRefs[idx]
            // A plain authority name ("ivan", "GROUP_all") parses into a ref with a blank sourceId,
            // which resolves to nothing - keeping it would only inflate the batches below. Only a
            // recipient that already looks like a ref is kept alongside its resolved form: for a
            // non-canonical one (e.g. "alfresco/people@admin") the resolved ref is what answers
            // email/personDisabled, and querying just one of the two would silently drop the
            // recipient from the mail.
            result[recipient] = if (localRef == resolvedRef || localRef.getSourceId().isBlank()) {
                setOf(resolvedRef)
            } else {
                linkedSetOf(localRef, resolvedRef)
            }
        }

        return result
    }
}

internal data class GroupInfo(
    @AttName("containedUsers")
    val containedUsers: List<EntityRef> = emptyList()
)

internal data class UserInfo(
    @AttName("email")
    var email: String? = "",
    @AttName("personDisabled")
    var disabled: Boolean? = false
)
