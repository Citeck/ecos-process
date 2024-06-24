package ru.citeck.ecos.process.domain.proctask.attssync

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.listener.ListenerHandle
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.Predicates.eq
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct

@Component
class MetaDataChangeTaskAttsSyncListener(
    private val eventsService: EventsService,
    private val procTaskAttsSyncService: ProcTaskAttsSyncService,
    private val taskAttsSynchronizer: ProcTaskAttsSynchronizer
) {

    private var listenerHandle: ListenerHandle? = null

    @Synchronized
    @PostConstruct
    fun updateListeners() {
        val syncSettings = procTaskAttsSyncService.findEnabledSyncSettingsForSource(TaskAttsSyncSource.RECORD)
        if (syncSettings.isEmpty()) {
            return
        }

        val typesToAttributes = syncSettings.toTypesByAttributes()
        val allPossibleTypes = typesToAttributes.keys.map { it.toString() }
        val allAttributes = typesToAttributes.values.map { it.values }.flatten().toSet()

        val predicateFilterByTypesAndAtts = Predicates.and(
            Predicates.or(
                Predicates.`in`("record._type?id", allPossibleTypes),
                Predicates.or(
                    *allPossibleTypes.map {
                        eq("record._type.isSubTypeOf.$it?bool", true)
                    }.toTypedArray()
                )
            ),
            Predicates.or(
                *allAttributes.map {
                    eq("diff._has.$it?bool!", true)
                }.toTypedArray()
            )
        )

        listenerHandle?.remove()
        listenerHandle = eventsService.addListener<RecordUpdatedEvent> {
            withEventType(RecordChangedEvent.TYPE)
            withDataClass(RecordUpdatedEvent::class.java)
            withFilter(predicateFilterByTypesAndAtts)
            withTransactional(true)
            withAction {
                if (it.record == null || it.typeId == null || it.after == null) {
                    return@withAction
                }

                AuthContext.runAsSystem {
                    taskAttsSynchronizer.syncOnDocumentChanged(
                        DocumentUpdatedInfo(
                            it.record,
                            EntityRef.valueOf("${AppName.EMODEL}/type@${it.typeId}"),
                            it.after
                        )
                    )
                }
            }
        }
    }

    private data class RecordUpdatedEvent(
        @AttName("record?id")
        val record: EntityRef? = null,

        @AttName("typeDef.id")
        val typeId: String? = null,

        @AttName("after")
        val after: Map<String, Any?>? = null
    )
}
