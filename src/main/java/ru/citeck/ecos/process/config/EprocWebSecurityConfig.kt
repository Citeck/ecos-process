package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain
import ru.citeck.ecos.context.lib.auth.AuthRole

@Order(90)
@Configuration
class EprocWebSecurityConfig {

    @Bean
    @Order(-200)
    fun camundaHttpSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {

        return http.securityMatcher("/engine-rest/**", "/camunda/**").authorizeHttpRequests {
            it.requestMatchers("/engine-rest/**").hasAnyAuthority(AuthRole.ADMIN, AuthRole.SYSTEM)
            it.requestMatchers("/camunda/**").hasAnyAuthority(AuthRole.ADMIN)
        }.build()
    }
}
