package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthGroup
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.model.lib.delegation.service.DelegationService
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecords
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class TaskActorsUtils(
    private val authorityService: EcosAuthoritiesApi,
    private val delegationService: DelegationService
) {

    companion object {
        private val log = KotlinLogging.logger {}
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

        val currentUserRef = authorityService.getAuthorityRef(currentUser)
        val currentAuthoritiesRefs = authorityService.getAuthorityRefs(currentAuthorities)

        log.trace { "Is task actor: \ntaskId=$taskId \nuser=$currentUserRef \nuserAuthorities=$currentAuthoritiesRefs" }

        if (assignee == currentUserRef) return true
        if (candidateUsers.contains(currentUserRef)) return true
        if (candidateGroups.any { it in currentAuthoritiesRefs }) return true

        if (documentType.isNullOrBlank()) {
            return false
        }
        val delegations = delegationService.getActiveAuthDelegations(
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
            val usersRefs = authorityService.getAuthorityRefs(users)
            val groupsRefs = authorityService.getAuthorityRefs(groups)
            if (usersRefs.contains(assignee)) return true
            if (usersRefs.any { candidateUsers.contains(it) }) return true
            if (groupsRefs.any { candidateGroups.contains(it) }) return true
        }

        return false
    }

    fun isCurrentUserTaskActorOrDelegate(procTaskDto: ProcTaskDto): Boolean {
        return isCurrentUserTaskActorOrDelegate(
            procTaskDto.id,
            procTaskDto.assignee,
            procTaskDto.candidateUsers,
            procTaskDto.candidateGroups,
            procTaskDto.documentType
        )
    }

    fun isCurrentUserTaskActorOrDelegate(procTaskRecord: ProcTaskRecords.ProcTaskRecord): Boolean {
        return isCurrentUserTaskActorOrDelegate(
            procTaskRecord.id,
            procTaskRecord.assignee,
            procTaskRecord.candidateUsers,
            procTaskRecord.candidateGroups,
            procTaskRecord.documentType
        )
    }
}
