package ru.citeck.ecos.process.domain.proctask.eapp

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_SOURCE_ID
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_TYPE
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Consumer

@Component
class ProcTaskAttsSyncArtifactHandler(
    private val recordsService: RecordsService,
    private val eventsService: EventsService
) : EcosArtifactHandler<ProcTaskAttsSync> {

    override fun deleteArtifact(artifactId: String) {
        recordsService.delete(
            EntityRef.create(
                AppName.EPROC,
                PROC_TASK_ATTS_SYNC_SOURCE_ID,
                artifactId
            )
        )
    }

    override fun getArtifactType(): String {
        return "process/$PROC_TASK_ATTS_SYNC_TYPE"
    }

    override fun listenChanges(listener: Consumer<ProcTaskAttsSync>) {
        listOf(RecordChangedEvent.TYPE, RecordCreatedEvent.TYPE).forEach { eventType ->
            eventsService.addListener<ProcTaskAttsSync> {
                withEventType(eventType)
                withDataClass(ProcTaskAttsSync::class.java)
                withFilter(Predicates.eq("typeDef.id", PROC_TASK_ATTS_SYNC_SOURCE_ID))
                withAction {
                    listener.accept(it)
                }
            }
        }
    }

    override fun deployArtifact(artifact: ProcTaskAttsSync) {
        recordsService.mutate(
            EntityRef.create(AppName.EPROC, PROC_TASK_ATTS_SYNC_SOURCE_ID, ""),
            artifact
        )
    }
}

class ProcTaskAttsSync(
    @AttName("record?localId")
    var id: String = "",

    @AttName("record.enabled?bool!")
    var enabled: Boolean = false,

    @AttName("record.name!")
    val name: String,

    @AttName("record.source!")
    val source: String,

    @AttName("record.attributesSync[]?json!")
    var attributesSync: List<DataValue>
)
