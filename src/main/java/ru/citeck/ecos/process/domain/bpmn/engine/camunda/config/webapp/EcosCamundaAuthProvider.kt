package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.webapp

import jakarta.servlet.http.HttpServletRequest
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.context.lib.auth.AuthUser

class EcosCamundaAuthProvider : ContainerBasedAuthenticationProvider() {

    private val deniedUsers = listOf(AuthUser.ANONYMOUS, AuthUser.GUEST, "anonymousUser")

    override fun extractAuthenticatedUser(request: HttpServletRequest, engine: ProcessEngine): AuthenticationResult {

        val currentFullAuth = AuthContext.getCurrentFullAuth()
        val user = currentFullAuth.getUser()
        if (currentFullAuth.isEmpty() || deniedUsers.contains(user)) {
            return AuthenticationResult.unsuccessful()
        }

        return AuthenticationResult(user, true).apply {
            groups = currentFullAuth.getAuthorities()
        }
    }
}
