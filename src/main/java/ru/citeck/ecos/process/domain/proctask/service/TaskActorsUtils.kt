package ru.citeck.ecos.process.domain.proctask.service

import mu.KotlinLogging
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.proctask.dto.ProcTaskDto

private val log = KotlinLogging.logger {}

fun currentUserIsTaskActor(task: ProcTaskDto): Boolean {
    val currentUser = AuthContext.getCurrentUser()
    val currentAuthorities = AuthContext.getCurrentAuthorities()

    return isTaskActor(task, currentUser, currentAuthorities)
}

fun isTaskActor(task: ProcTaskDto, user: String, userAuthorities: List<String>): Boolean {
    log.debug { "Is task actor: taskId=${task.id} user=$user userAuthorities=$userAuthorities task:\n$task" }

    val assigneeName = task.assignee.id
    val candidateUsers = task.candidateUsers.map { it.id }
    val candidateGroup = task.candidateGroups.map { it.id }

    if (assigneeName == user) return true
    if (candidateUsers.contains(user)) return true
    if (candidateGroup.any { it in userAuthorities }) return true

    return false
}
