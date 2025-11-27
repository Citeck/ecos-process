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

    private val userWorkspaces = ConcurrentHashMap<String, Set<String>>()

    @PostConstruct
    fun init() {
        modelServiceFactory.setWorkspaceApi(this)
    }

    override fun getNestedWorkspaces(workspaces: Collection<String>): List<Set<String>> {
        return workspaces.map { emptySet() }
    }

    override fun getUserWorkspaces(user: String, membershipType: WsMembershipType): Set<String> {
        return userWorkspaces[user] ?: emptySet()
    }

    override fun isUserManagerOf(user: String, workspace: String): Boolean {
        return false
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
        this.userWorkspaces[user] = workspaces
    }

    fun cleanUp() {
        userWorkspaces.clear()
    }
}
