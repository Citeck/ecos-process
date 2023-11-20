package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config.webapp

import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.boot.web.servlet.ServletContextInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*
import java.util.regex.Pattern
import javax.servlet.*
import javax.servlet.http.HttpServletRequest

@Configuration
class CamundaWebAppConfiguration {

    companion object {
        private const val CSRF_PREVENTION_FILTER = "CsrfPreventionFilter"
    }

    /**
     * Overwrite csrf filter from Camunda configured here
     * org.camunda.bpm.spring.boot.starter.webapp.CamundaBpmWebappInitializer
     * org.camunda.bpm.spring.boot.starter.webapp.filter.SpringBootCsrfPreventionFilter
     * Is configured with basically a 'no-op' filter
     */
    @Bean
    fun camundaCsrfOverwrite(): ServletContextInitializer {
        return ServletContextInitializer { servletContext: ServletContext ->
            servletContext.addFilter(
                CSRF_PREVENTION_FILTER,
                (
                    object : Filter {
                        override fun doFilter(
                            request: ServletRequest,
                            response: ServletResponse,
                            chain: FilterChain
                        ) {
                            chain.doFilter(request, response)
                        }

                        override fun init(filterConfig: FilterConfig) {}
                        override fun destroy() {}
                    }
                    )
            )
        }
    }

    @Bean
    fun containerBasedAuthenticationFilter(): FilterRegistrationBean<*> {
        val filterRegistration = FilterRegistrationBean<ContainerBasedAuthenticationFilter>()
        filterRegistration.setFilter(EcosContainerBasedAuthenticationFilter())
        filterRegistration.setInitParameters(
            Collections.singletonMap(
                "authentication-provider",
                EcosCamundaAuthProvider::class.java.canonicalName
            )
        )
        filterRegistration.order = 101 // make sure the filter is registered after the Spring Security Filter Chain
        filterRegistration.addUrlPatterns("/camunda/app/*", "/camunda/api/*", "/camunda/lib/*")
        return filterRegistration
    }
}

class EcosContainerBasedAuthenticationFilter : ContainerBasedAuthenticationFilter() {

    companion object {
        private val APP_PATTERN = Pattern.compile("/api/admin/auth/user/([^/]+).*")
    }

    override fun extractEngineName(request: HttpServletRequest): String? {
        val originalName: String? = super.extractEngineName(request)
        if (!originalName.isNullOrEmpty()) {
            return originalName
        }

        val url = getRequestUri(request)
        val apiEngineMatcher = APP_PATTERN.matcher(url)
        if (apiEngineMatcher.matches()) {
            return apiEngineMatcher.group(1)
        }

        return null
    }
}
