package ru.citeck.ecos.process.domain.bpmn.listener

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.camunda.bpm.engine.TaskService
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerConfig
import ru.citeck.ecos.events2.type.RecordRefChangedEvent
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaMyBatisExtension
import ru.citeck.ecos.process.domain.proctask.converter.CacheableTaskConverter
import ru.citeck.ecos.webapp.api.entity.EntityRef

/**
 * Listens for [RecordRefChangedEvent] and updates the `documentRef` process variable
 * in all active Camunda process instances that reference the old record ref.
 *
 * This keeps running processes in sync when a record ref is changed.
 */
@Component
class RecordRefChangedListener(
    private val eventsService: EventsService,
    private val camundaMyBatisExtension: CamundaMyBatisExtension,
    private val camundaTaskService: TaskService,
    private val cacheableTaskConverter: CacheableTaskConverter
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @PostConstruct
    fun init() {
        eventsService.addListener(
            ListenerConfig.create<RefChangedData>()
                .withEventType(RecordRefChangedEvent.TYPE)
                .withTransactional(true)
                .withDataClass(RefChangedData::class.java)
                .withAction { event ->
                    log.debug { "RecordRefChangedEvent received: ${event.before} -> ${event.after}" }

                    val affectedTaskIds = camundaTaskService.createTaskQuery()
                        .processVariableValueEquals(BPMN_DOCUMENT_REF, event.before.toString())
                        .list()
                        .map { task -> task.id }

                    val updatedVars: Int
                    val updatedKeys: Int
                    val updatedSubs: Int
                    camundaMyBatisExtension.run {
                        updatedVars = updateDocumentRefVariables(event.before, event.after)
                        updatedKeys = updateDocumentRefInBusinessKey(event.before, event.after)
                        updatedSubs = updateDocumentRefInEventSubscriptions(event.before, event.after)
                    }

                    affectedTaskIds.forEach { taskId ->
                        cacheableTaskConverter.removeFromActualTaskCache(taskId)
                    }
                    if (updatedVars > 0 || updatedKeys > 0 || updatedSubs > 0 || affectedTaskIds.isNotEmpty()) {
                        log.info {
                            "RecordRefChangedEvent processed: " +
                                "$updatedVars variable(s), $updatedKeys business key(s), " +
                                "$updatedSubs event subscription(s), " +
                                "${affectedTaskIds.size} task cache(s) evicted: " +
                                "${event.before} -> ${event.after}"
                        }
                    }
                }.build()
        )
    }

    class RefChangedData(
        val before: EntityRef,
        val after: EntityRef
    )
}
