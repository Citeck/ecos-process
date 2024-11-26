package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.webapp

import jakarta.servlet.http.HttpServletRequest
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider
import ru.citeck.ecos.context.lib.auth.AuthContext

class EcosCamundaAuthProvider : ContainerBasedAuthenticationProvider() {

    override fun extractAuthenticatedUser(request: HttpServletRequest, engine: ProcessEngine): AuthenticationResult {
        if (AuthContext.isRunAsAdmin()) {
            return AuthenticationResult("admin", true).apply {
                val currentFullAuth = AuthContext.getCurrentFullAuth()
                groups = currentFullAuth.getAuthorities()
            }
        }

        return AuthenticationResult.unsuccessful()
    }
}
