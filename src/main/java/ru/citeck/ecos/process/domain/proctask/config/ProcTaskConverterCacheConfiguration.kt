package ru.citeck.ecos.process.domain.proctask.config

import com.hazelcast.config.MapConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.webapp.lib.spring.autoconfigure.HazelcastMapConfig

const val PROC_TASKS_DTO_CONVERTER_CACHE_KEY = "proc-task-dto-by-ids-convert-cache"
const val PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY = "proc-historic-task-dto-by-ids-convert-cache"

/**
 * @author Roman Makarskiy
 */
@Configuration
class ProcTaskConverterCacheConfiguration(
    @Qualifier("taskDtoConverterCacheConfig")
    val mapConfig: MapConfig
) : HazelcastMapConfig {

    override fun getKey(): String {
        return PROC_TASKS_DTO_CONVERTER_CACHE_KEY
    }

    override fun getConfig(): MapConfig {
        return mapConfig
    }
}

@Configuration
class ProcHistoricTaskConverterCacheConfiguration(
    @Qualifier("historicTaskDtoConverterCacheConfig")
    val mapConfig: MapConfig
) : HazelcastMapConfig {

    override fun getKey(): String {
        return PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY
    }

    override fun getConfig(): MapConfig {
        return mapConfig
    }
}

@Configuration
class TaskDtoConverterCacheProperties {

    @Bean
    @ConfigurationProperties(prefix = "ecos-process.tasks.dto-converter.cache.hazelcast")
    fun taskDtoConverterCacheConfig(): MapConfig {
        return MapConfig().apply {
            name = PROC_TASKS_DTO_CONVERTER_CACHE_KEY
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "ecos-process.tasks.historic-dto-converter.cache.hazelcast")
    fun historicTaskDtoConverterCacheConfig(): MapConfig {
        return MapConfig().apply {
            name = PROC_HISTORIC_TASKS_DTO_CONVERTER_CACHE_KEY
        }
    }
}
