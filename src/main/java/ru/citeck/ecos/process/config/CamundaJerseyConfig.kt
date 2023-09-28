package ru.citeck.ecos.process.config

import org.camunda.bpm.spring.boot.starter.rest.CamundaJerseyResourceConfig
import org.springframework.context.annotation.Configuration
import java.io.IOException
import javax.ws.rs.ApplicationPath
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerResponseContext
import javax.ws.rs.container.ContainerResponseFilter
import javax.ws.rs.core.MultivaluedMap

// TODO: remove
@Configuration
@ApplicationPath("/engine-rest")
class CamundaJerseyConfig : CamundaJerseyResourceConfig() {

    init {
        register(CORSResponseFilter::class.java)
    }

    internal class CORSResponseFilter : ContainerResponseFilter {
        @Throws(IOException::class)
        override fun filter(requestContext: ContainerRequestContext, responseContext: ContainerResponseContext) {
            val headers: MultivaluedMap<String, Any> = responseContext.headers

            // Здесь нужно указать домен, с которого разрешено обращаться к API. Получить можно из [EcosWebAppProps.webUrl]
            headers.add("Access-Control-Allow-Origin", "http://localhost:7070")
            headers.add("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
            headers.add("Access-Control-Allow-Headers", "X-Requested-With, Content-Type, X-Codingpedia")
            headers.add("Access-Control-Allow-Credentials", "true")
        }
    }

}
