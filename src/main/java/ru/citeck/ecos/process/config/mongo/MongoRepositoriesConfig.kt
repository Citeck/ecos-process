package ru.citeck.ecos.process.config.mongo

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
@ConditionalOnProperty(value = [MongoDisabledEnvironmentPostProcessor.MONGO_ENABLED_PROP], havingValue = "true", matchIfMissing = true)
@EnableMongoRepositories("ru.citeck.ecos.process.domain.*.repo")
class MongoRepositoriesConfig
