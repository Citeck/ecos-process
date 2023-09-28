package ru.citeck.ecos.process.config

// TODO: uncomment after

//import org.springframework.context.annotation.Configuration
//import org.springframework.core.annotation.Order
//import org.springframework.security.config.annotation.web.builders.HttpSecurity
//import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
//import ru.citeck.ecos.context.lib.auth.AuthRole
//import ru.citeck.ecos.webapp.lib.spring.context.security.SecurityConfiguration
//
//@Order(90)
//@Configuration
//class EprocWebSecurityConfig(
//    val existingConfig: SecurityConfiguration
//) : WebSecurityConfigurerAdapter() {
//
//    @Throws(Exception::class)
//    override fun configure(http: HttpSecurity) {
//        existingConfig.configure(http)
//
//        http
//            .authorizeRequests()
//            .antMatchers("/engine-rest/**").hasAnyAuthority(AuthRole.ADMIN, AuthRole.SYSTEM)
//    }
//}
