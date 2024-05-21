package ru.citeck.ecos.process.domain.bpmn.process

import com.hazelcast.config.MapConfig
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.cache.annotation.Cacheable
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.spring.autoconfigure.HazelcastMapConfig

const val FIRST_ENABLED_PROCESS_FINDER_CACHE_KEY = "resolve-first-enabled-proc-def-by-type-hierarchy"

@Configuration
class FirstEnabledProcessFinderCacheConfiguration(
    @Qualifier("firstEnabledProcDefFinderCacheConfig")
    val mapConfig: MapConfig
) : HazelcastMapConfig {
    override fun getKey(): String {
        return FIRST_ENABLED_PROCESS_FINDER_CACHE_KEY
    }

    override fun getConfig(): MapConfig {
        return mapConfig
    }
}

@Configuration
class FirstEnabledProcessFinderCacheProperties {
    @Bean
    @ConfigurationProperties(prefix = "ecos-process.bpmn.first-enabled-proc-def.cache.hazelcast")
    fun firstEnabledProcDefFinderCacheConfig(): MapConfig {
        return MapConfig().apply {
            name = FIRST_ENABLED_PROCESS_FINDER_CACHE_KEY
        }
    }
}

@Component
class CachedFirstEnabledProcessDefFinder(
    private val procDefService: ProcDefService
) {

    @Cacheable(cacheNames = [FIRST_ENABLED_PROCESS_FINDER_CACHE_KEY])
    fun find(type: String): ProcDefWithDataDto? {
        val procRev = procDefService.findProcDef(BPMN_PROC_TYPE, type.toEntityRef(), emptyList()) ?: return null
        return procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, procRev.procDefId))
    }
}
