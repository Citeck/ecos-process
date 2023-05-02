package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.api.records.ProcTaskRecord
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.webapp.api.authority.EcosAuthoritiesApi
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct

@Component
class TaskActorsUtils(
    val authorityService: EcosAuthoritiesApi
) {
    @PostConstruct
    private fun init() {
        utils = this
    }
}

private lateinit var utils: TaskActorsUtils

private val log = KotlinLogging.logger {}

fun ProcTaskDto.currentUserIsTaskActor(): Boolean {
    return isTaskActor(id, assignee, candidateUsers, candidateGroups)
}

fun ProcTaskRecord.currentUserIsTaskActor(): Boolean {
    return isTaskActor(id, assignee, candidateUsers, candidateGroups)
}

private fun isTaskActor(
    taskId: String,
    assignee: EntityRef,
    candidateUsers: List<EntityRef>,
    candidateGroups: List<EntityRef>
): Boolean {
    val currentUser = AuthContext.getCurrentUser()
    val currentAuthorities = AuthContext.getCurrentAuthorities()

    val currentUserRef = utils.authorityService.getAuthorityRef(currentUser)
    val currentAuthoritiesRefs = utils.authorityService.getAuthorityRefs(currentAuthorities)

    log.trace { "Is task actor: \ntaskId=$taskId \nuser=$currentUserRef \nuserAuthorities=$currentAuthoritiesRefs" }

    if (assignee == currentUserRef) return true
    if (candidateUsers.contains(currentUserRef)) return true
    if (candidateGroups.any { it in currentAuthoritiesRefs }) return true

    return false
}
