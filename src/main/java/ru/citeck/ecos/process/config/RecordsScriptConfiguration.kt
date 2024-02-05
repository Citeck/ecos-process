package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService

/**
 * @author Roman Makarskiy
 */
@Configuration
class RecordsScriptConfiguration {

    @Bean
    fun recordsScriptService(services: RecordsServiceFactory): RecordsScriptService {
        return RecordsScriptService(services)
    }
}
