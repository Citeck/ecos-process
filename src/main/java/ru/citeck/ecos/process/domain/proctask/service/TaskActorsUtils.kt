package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto
import ru.citeck.ecos.webapp.api.authority.EcosAuthorityService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct

@Component
class TaskActorsUtils(
    val authorityService: EcosAuthorityService
) {
    @PostConstruct
    private fun init() {
        utils = this
    }
}

private lateinit var utils: TaskActorsUtils

private val log = KotlinLogging.logger {}

fun currentUserIsTaskActor(task: ProcTaskDto): Boolean {
    val currentUser = AuthContext.getCurrentUser()
    val currentAuthorities = AuthContext.getCurrentAuthorities()

    val currentUserRef = utils.authorityService.getAuthorityRef(currentUser)
    val currentAuthoritiesRefs = utils.authorityService.getAuthorityRefs(currentAuthorities)

    return isTaskActor(task, currentUserRef, currentAuthoritiesRefs)
}

private fun isTaskActor(task: ProcTaskDto, user: EntityRef, userAuthorities: List<EntityRef>): Boolean {
    log.debug { "Is task actor: taskId=${task.id} user=$user userAuthorities=$userAuthorities task:\n$task" }

    if (task.assignee == user) return true
    if (task.candidateUsers.contains(user)) return true
    if (task.candidateGroups.any { it in userAuthorities }) return true

    return false
}
