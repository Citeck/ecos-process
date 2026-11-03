package ru.citeck.ecos.process.domain.proctask.attssync

import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class ProcTaskSyncJobScheduler(private val synchronizer: ProcTaskAttsSynchronizer) {
    @Scheduled(fixedDelayString = "\${ecos-process.bpmn.task-atts-sync.job-rate-ms:1000}")
    fun execute() {
        synchronizer.processPendingSyncs()
    }
}
