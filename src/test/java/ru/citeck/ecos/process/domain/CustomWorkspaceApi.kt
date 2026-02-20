package ru.citeck.ecos.process.domain

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.workspace.api.WorkspaceApi
import ru.citeck.ecos.model.lib.workspace.api.WsMembershipType
import java.util.concurrent.ConcurrentHashMap

@Component
class CustomWorkspaceApi(
    val modelServiceFactory: ModelServiceFactory
) : WorkspaceApi {

    companion object {
        const val WS_SYS_ID_POSTFIX = "-sys-id"
    }

    private val userMemberships = ConcurrentHashMap<String, List<WsMembership>>()

    @PostConstruct
    fun init() {
        modelServiceFactory.setWorkspaceApi(this)
    }

    override fun getNestedWorkspaces(workspaces: Collection<String>): List<Set<String>> {
        return workspaces.map { emptySet() }
    }

    override fun getUserWorkspaces(user: String, membershipType: WsMembershipType): Set<String> {
        return userMemberships[user]?.mapTo(LinkedHashSet()) { it.workspace } ?: emptySet()
    }

    override fun isUserManagerOf(user: String, workspace: String): Boolean {
        return userMemberships[user]?.any { it.workspace == workspace && it.isManager } == true
    }

    override fun mapIdentifiers(identifiers: List<String>, mappingType: WorkspaceApi.IdMappingType): List<String> {
        return when (mappingType) {
            WorkspaceApi.IdMappingType.WS_SYS_ID_TO_ID -> identifiers.map {
                if (it.endsWith(WS_SYS_ID_POSTFIX)) {
                    it.substring(0, it.length - WS_SYS_ID_POSTFIX.length)
                } else {
                    it
                }
            }
            WorkspaceApi.IdMappingType.WS_ID_TO_SYS_ID -> identifiers.map {
                if (it.isEmpty()) {
                    it
                } else {
                    it + WS_SYS_ID_POSTFIX
                }
            }
            WorkspaceApi.IdMappingType.NO_MAPPING -> identifiers
        }
    }

    fun setUserWorkspaces(user: String, workspaces: Set<String>) {
        this.userMemberships[user] = workspaces.map { WsMembership(it) }
    }

    fun setUserMemberships(user: String, memberships: List<WsMembership>) {
        this.userMemberships[user] = memberships
    }

    fun cleanUp() {
        userMemberships.clear()
    }

    data class WsMembership(
        val workspace: String,
        val isManager: Boolean = false
    )
}
