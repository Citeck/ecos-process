package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.delegation.service.DelegationService
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecord
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct

@Component
class TaskActorsUtils(
    val authorityService: EcosAuthoritiesApi,
    val delegationService: DelegationService
) {
    @PostConstruct
    private fun init() {
        utils = this
    }
}

private lateinit var utils: TaskActorsUtils

private val log = KotlinLogging.logger {}

fun ProcTaskDto.isCurrentUserTaskActorOrDelegate(): Boolean {
    return isCurrentUserTaskActorOrDelegate(id, assignee, candidateUsers, candidateGroups, documentType)
}

fun ProcTaskRecord.isCurrentUserTaskActorOrDelegate(): Boolean {
    return isCurrentUserTaskActorOrDelegate(id, assignee, candidateUsers, candidateGroups, documentType)
}

private fun isCurrentUserTaskActorOrDelegate(
    taskId: String,
    assignee: EntityRef,
    candidateUsers: List<EntityRef>,
    candidateGroups: List<EntityRef>,
    documentType: String?
): Boolean {
    val currentUser = AuthContext.getCurrentUser()
    val currentAuthorities = AuthContext.getCurrentAuthorities()

    val currentUserRef = utils.authorityService.getAuthorityRef(currentUser)
    val currentAuthoritiesRefs = utils.authorityService.getAuthorityRefs(currentAuthorities)

    log.trace { "Is task actor: \ntaskId=$taskId \nuser=$currentUserRef \nuserAuthorities=$currentAuthoritiesRefs" }

    if (assignee == currentUserRef) return true
    if (candidateUsers.contains(currentUserRef)) return true
    if (candidateGroups.any { it in currentAuthoritiesRefs }) return true

    if (documentType.isNullOrBlank()) {
        return false
    }
    val delegations = utils.delegationService.getActiveAuthDelegations(
        currentUser,
        listOf(documentType)
    )
    for (delegation in delegations) {
        val users = delegation.delegatedAuthorities.filter {
            !it.startsWith(AuthGroup.PREFIX) && !it.startsWith(AuthRole.PREFIX)
        }
        val groups = delegation.delegatedAuthorities.filter {
            it.startsWith(AuthGroup.PREFIX)
        }
        val usersRefs = utils.authorityService.getAuthorityRefs(users)
        val groupsRefs = utils.authorityService.getAuthorityRefs(groups)
        if (usersRefs.contains(assignee)) return true
        if (usersRefs.any { candidateUsers.contains(it) }) return true
        if (groupsRefs.any { candidateGroups.contains(it) }) return true
    }

    return false
}
