package ru.citeck.ecos.process.domain.bpmnla.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.lazyapproval.api.BpmnLazyApprovalRemoteApi
import ru.citeck.ecos.lazyapproval.api.LazyApprovalRecordsDao
import ru.citeck.ecos.records3.RecordsService

@Configuration
class LazyApprovalConfig {

    @Bean
    fun lazyApprovalRecordsDao(
        recordsService: RecordsService,
        bpmnLazyApprovalService: BpmnLazyApprovalRemoteApi
    ): LazyApprovalRecordsDao {
        return LazyApprovalRecordsDao(recordsService, bpmnLazyApprovalService)
    }
}
