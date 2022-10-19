package ru.citeck.ecos.process.domain.procdef.events

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.emitter.EmitterConfig

private const val PROC_DEF_EVENT_CREATE = "proc-def-create"
private const val PROC_DEF_EVENT_UPDATE = "proc-def-update"
private const val PROC_DEF_EVENT_DEPLOYED = "proc-def-deployed"

@Component
class ProcDefEventEmitter(
    eventsService: EventsService,

    @Value("\${spring.application.name}")
    private val appName: String
) {

    private val procDefCreateEmitter = eventsService.getEmitter(
        EmitterConfig.create<ProcDefEvent> {
            source = appName
            eventType = PROC_DEF_EVENT_CREATE
            eventClass = ProcDefEvent::class.java
        }
    )

    private val procDefUpdateEmitter = eventsService.getEmitter(
        EmitterConfig.create<ProcDefEvent> {
            source = appName
            eventType = PROC_DEF_EVENT_UPDATE
            eventClass = ProcDefEvent::class.java
        }
    )

    private val procDefDeployedEmitter = eventsService.getEmitter(
        EmitterConfig.create<ProcDefEvent> {
            source = appName
            eventType = PROC_DEF_EVENT_DEPLOYED
            eventClass = ProcDefEvent::class.java
        }
    )

    fun emitProcDefCreate(event: ProcDefEvent) {
        procDefCreateEmitter.emit(event)
    }

    fun emitProcDefUpdate(event: ProcDefEvent) {
        procDefUpdateEmitter.emit(event)
    }

    fun emitProcDefDeployed(event: ProcDefEvent) {
        procDefDeployedEmitter.emit(event)
    }

}
