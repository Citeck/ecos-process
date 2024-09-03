package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.webapp.lib.spring.context.webmvc.initEcosSecurity

@Order(90)
@Configuration
class EprocWebSecurityConfig {

    @Bean
    @Order(-200)
    fun camundaHttpSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http.initEcosSecurity().securityMatcher(
            // Simple matcher by securityMatcher("/engine-rest/**", ...) doesn't work
            // because it drops first part from request URI and tries to apply filter for it.
            // E.g. instead of call matcher for /engine-rest/metrics it will be called for /metrics
            OrRequestMatcher(
                AntPathRequestMatcher.antMatcher("/engine-rest/**"),
                AntPathRequestMatcher.antMatcher("/camunda/**")
            )
        ).authorizeHttpRequests {
            it.requestMatchers("/engine-rest/**").hasAnyAuthority(AuthRole.ADMIN, AuthRole.SYSTEM)
                .requestMatchers("/camunda/**").hasAnyAuthority(AuthRole.ADMIN)
        }.build()
    }
}
