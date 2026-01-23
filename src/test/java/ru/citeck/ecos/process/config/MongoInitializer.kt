package ru.citeck.ecos.process.config

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextStoppedEvent
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import java.time.Instant

@Testcontainers
class MongoInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Container
    val container = MongoDBContainer(DockerImageName.parse("mongo:4.2"))

    init {
        container.start()
    }

    override fun initialize(context: ConfigurableApplicationContext) {
        val mongoUrl = container.replicaSetUrl
        //  Logging is not initialized at this stage
        println("${Instant.now()} -- Initializing MongoDB test container with url: $mongoUrl")
        TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
            context,
            "spring.data.mongodb.uri=$mongoUrl"
        )
        context.addApplicationListener {
            if (it is ContextClosedEvent || it is ContextStoppedEvent) {
                container.stop()
            }
        }
    }
}
