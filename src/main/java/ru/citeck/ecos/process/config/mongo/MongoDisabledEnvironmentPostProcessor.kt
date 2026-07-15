package ru.citeck.ecos.process.config.mongo

import org.springframework.boot.SpringApplication
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.env.EnvironmentPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.getProperty

/**
 * Spring Boot creates MongoClient bean from autoconfiguration regardless of any @ConditionalOnProperty
 * on application beans, and mongo driver starts to connect to the host in background.
 * The only way to switch auto configurations off by property value is to exclude them from the environment.
 *
 * The order is the lowest precedence on purpose: this post processor must run after every source of
 * [MONGO_ENABLED_PROP] is loaded into the environment (application.yml is loaded by
 * ConfigDataEnvironmentPostProcessor, which is ordered HIGHEST_PRECEDENCE + 10). Reading the property
 * too early would silently fall back to the default value 'true' and leave mongo auto configurations
 * enabled, so the driver would keep connecting to the host in background.
 */
class MongoDisabledEnvironmentPostProcessor :
    EnvironmentPostProcessor,
    Ordered {

    override fun getOrder(): Int {
        return Ordered.LOWEST_PRECEDENCE
    }

    companion object {
        const val MONGO_ENABLED_PROP = "ecos-process.mongo.enabled"

        private const val EXCLUDE_PROP = "spring.autoconfigure.exclude"
        private const val PROPERTY_SOURCE_NAME = "mongo-disabled-auto-configuration-excludes"

        private val MONGO_AUTO_CONFIGURATIONS = listOf(
            "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration",
            "org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration",
            "org.springframework.boot.autoconfigure.data.mongo.MongoReactiveRepositoriesAutoConfiguration"
        )
    }

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {

        if (environment.getProperty(MONGO_ENABLED_PROP, true)) {
            return
        }

        val excludes = LinkedHashSet<String>()
        Binder.get(environment)
            .bind(EXCLUDE_PROP, Array<String>::class.java)
            .orElse(emptyArray())
            .map { it.trim() }
            .filterTo(excludes) { it.isNotEmpty() }
        excludes.addAll(MONGO_AUTO_CONFIGURATIONS)

        environment.propertySources.addFirst(
            MapPropertySource(
                PROPERTY_SOURCE_NAME,
                mapOf(EXCLUDE_PROP to excludes.joinToString(","))
            )
        )
    }
}
