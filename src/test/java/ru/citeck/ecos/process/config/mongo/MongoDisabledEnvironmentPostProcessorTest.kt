package ru.citeck.ecos.process.config.mongo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.MapPropertySource
import org.springframework.mock.env.MockEnvironment

class MongoDisabledEnvironmentPostProcessorTest {

    private val postProcessor = MongoDisabledEnvironmentPostProcessor()
    private val application = SpringApplication()

    @Test
    fun `should exclude mongo auto configurations when mongo is disabled`() {
        val env = MockEnvironment()
        env.setProperty("ecos-process.mongo.enabled", "false")

        postProcessor.postProcessEnvironment(env, application)

        val excluded = env.getProperty("spring.autoconfigure.exclude", "")
        assertThat(excluded)
            .contains("org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
            .contains("org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration")
            .contains("org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration")
    }

    @Test
    fun `should not exclude anything when mongo is enabled`() {
        val env = MockEnvironment()
        env.setProperty("ecos-process.mongo.enabled", "true")

        postProcessor.postProcessEnvironment(env, application)

        assertThat(env.getProperty("spring.autoconfigure.exclude")).isNull()
    }

    @Test
    fun `should not exclude anything by default`() {
        val env = MockEnvironment()

        postProcessor.postProcessEnvironment(env, application)

        assertThat(env.getProperty("spring.autoconfigure.exclude")).isNull()
    }

    @Test
    fun `should keep already excluded auto configurations`() {
        val env = MockEnvironment()
        env.setProperty("ecos-process.mongo.enabled", "false")
        env.setProperty("spring.autoconfigure.exclude", "com.example.SomeAutoConfiguration")

        postProcessor.postProcessEnvironment(env, application)

        val excluded = env.getProperty("spring.autoconfigure.exclude", "")
        assertThat(excluded)
            .contains("com.example.SomeAutoConfiguration")
            .contains("org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
    }

    @Test
    fun `should keep already excluded auto configurations defined as yaml list`() {
        val env = MockEnvironment()
        env.setProperty("ecos-process.mongo.enabled", "false")
        env.propertySources.addFirst(
            MapPropertySource(
                "yaml-list-excludes",
                mapOf(
                    "spring.autoconfigure.exclude[0]" to "com.example.FooAutoConfiguration",
                    "spring.autoconfigure.exclude[1]" to "com.example.BarAutoConfiguration"
                )
            )
        )

        postProcessor.postProcessEnvironment(env, application)

        val excluded = Binder.get(env).bind("spring.autoconfigure.exclude", Array<String>::class.java).get()
        assertThat(excluded)
            .contains("com.example.FooAutoConfiguration")
            .contains("com.example.BarAutoConfiguration")
            .contains("org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration")
    }
}
