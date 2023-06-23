package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

//TODO: remove
// @Configuration
// class CamundaSecurityConfig {
//
//    @Bean
//    fun processEngineAuthenticationFilter(): FilterRegistrationBean<ProcessEngineAuthenticationFilter>? {
//        val registration = FilterRegistrationBean<ProcessEngineAuthenticationFilter>()
//        registration.setName("camunda-auth")
//        registration.filter = getProcessEngineAuthenticationFilter()
//        registration
//            .addInitParameter("authentication-provider", HttpBasicAuthenticationProvider::class.java.name)
//        registration.addUrlPatterns("/engine-rest/*")
//        return registration
//    }
//
//    @Bean
//    fun getProcessEngineAuthenticationFilter(): ProcessEngineAuthenticationFilter {
//        return ProcessEngineAuthenticationFilter()
//    }
//
// }
