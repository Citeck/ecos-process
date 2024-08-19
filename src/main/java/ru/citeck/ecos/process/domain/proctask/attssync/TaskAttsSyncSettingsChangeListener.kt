package ru.citeck.ecos.process.domain.proctask.attssync

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.model.lib.type.dto.TypeInfo
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_TYPE
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class TaskAttsSyncSettingsChangeListener(
    eventsService: EventsService,
    private val procTaskAttsSynchronizer: ProcTaskAttsSynchronizer,
    private val procTaskSyncCache: ProcTaskSyncCache,
    private val metaDataChangeTaskAttsSyncListener: MetaDataChangeTaskAttsSyncListener
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    init {
        eventsService.addListener<RecordUpdateEvent> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdateEvent::class.java)
            withFilter(
                Predicates.and(
                    Predicates.eq(
                        "typeDef.id",
                        PROC_TASK_ATTS_SYNC_TYPE
                    ),
                    Predicates.or(
                        Predicates.eq("diff._has.attributesSync?bool!", true),
                        Predicates.eq("diff._has.source?bool!", true),
                        Predicates.eq("diff._has.enabled?bool!", true)
                    )
                )
            )
            withTransactional(true)
            withAction {
                log.debug { "Task Atts Sync Settings was changed: ${it.record}" }

                procTaskSyncCache.evictTaskSyncAttributesCache()
                metaDataChangeTaskAttsSyncListener.updateListeners()

                TxnContext.doAfterCommit(100f, true) { ->
                    AuthContext.runAsSystem {
                        if (it.record == null || it.record.isEmpty()) {
                            log.warn { "Task Atts Sync Settings was changed, but record is empty: ${it.record}" }
                            return@runAsSystem
                        }

                        procTaskAttsSynchronizer.fullSyncForSettingsAsync(it.record)
                    }
                }
            }
        }

        eventsService.addListener<RecordCreateEvent> {
            withEventType(RecordCreatedEvent.TYPE)
            withDataClass(RecordCreateEvent::class.java)
            withFilter(
                Predicates.and(
                    Predicates.eq(
                        "typeDef.id",
                        PROC_TASK_ATTS_SYNC_TYPE
                    )
                )
            )
            withTransactional(true)
            withAction {
                log.debug { "Task Atts Sync Settings was created: ${it.record}" }

                procTaskSyncCache.evictTaskSyncAttributesCache()
                metaDataChangeTaskAttsSyncListener.updateListeners()

                TxnContext.doAfterCommit(100f, true) { ->
                    AuthContext.runAsSystem {
                        if (it.record == null || it.record.isEmpty()) {
                            log.warn { "Task Atts Sync Settings was created, but record is empty: ${it.record}" }
                            return@runAsSystem
                        }

                        procTaskAttsSynchronizer.fullSyncForSettingsAsync(it.record)
                    }
                }
            }
        }

        eventsService.addListener<RecordUpdateEvent> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdateEvent::class.java)
            withFilter(
                Predicates.and(
                    Predicates.eq(
                        "record._type?id",
                        "emodel/type@type"
                    )
                )
            )
            withTransactional(true)
            withAction {
                log.debug { "Type <${it.record}> was changed: ${it.changed}" }

                procTaskSyncCache.evictTaskSyncAttributesCache()

                if (it.record == null || it.record.isEmpty()) {
                    log.warn { "Type was changed, but type record are empty: ${it.changed}" }
                    return@withAction
                }

                TxnContext.doAfterCommit(100f, true) { ->
                    AuthContext.runAsSystem {
                        procTaskAttsSynchronizer.fullSyncForTypeAsync(it.record)
                    }
                }
            }
        }
    }

    private data class RecordCreateEvent(
        @AttName("record?id")
        val record: EntityRef? = null
    )

    private data class RecordUpdateEvent(
        @AttName("record?id")
        val record: EntityRef? = null,

        @AttName("diff.list[].id")
        val changed: List<String>? = null,

        @AttName("typeDef?json")
        val t: TypeInfo? = null
    )
}
