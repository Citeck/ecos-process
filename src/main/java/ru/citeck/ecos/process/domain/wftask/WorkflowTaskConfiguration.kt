package ru.citeck.ecos.process.domain.wftask

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.records3.record.dao.RecordsDao
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy

@Configuration
class WorkflowTaskConfiguration {

    @Bean
    fun wfTaskRecordsDao(): RecordsDao {
        return RecordsDaoProxy("wftask", "alfresco/wftask", null)
    }
}
