package ru.citeck.ecos.process.domain.bpmn.kpi.eapp

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.events2.type.RecordChangedEvent
import ru.citeck.ecos.events2.type.RecordCreatedEvent
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnDurationKpiTimeType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiSettings
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.util.function.Consumer

@Component
class BpmnKpiSettingsArtifactHandler(
    private val recordsService: RecordsService,
    private val eventsService: EventsService
) : EcosArtifactHandler<BpmnKpiSettings> {

    override fun deleteArtifact(artifactId: String) {
        recordsService.delete(
            EntityRef.create(
                AppName.EMODEL,
                BPMN_KPI_SETTINGS_SOURCE_ID,
                artifactId
            )
        )
    }

    override fun getArtifactType(): String {
        return "process/bpmn-kpi-settings"
    }

    override fun listenChanges(listener: Consumer<BpmnKpiSettings>) {
        listOf(RecordChangedEvent.TYPE, RecordCreatedEvent.TYPE).forEach { eventType ->
            eventsService.addListener<BpmnKpiSettingsAtts> {
                withEventType(eventType)
                withDataClass(BpmnKpiSettingsAtts::class.java)
                withFilter(Predicates.eq("typeDef.id", "bpmn-kpi-settings"))
                withAction {
                    println("listenChanges: ${it.toPrettyString()}")

                    listener.accept(
                        BpmnKpiSettings(
                            id = it.id,
                            name = it.name,
                            kpiType = it.kpiType,
                            enabled = it.enabled,
                            processRef = it.processRef.toEntityRef(),
                            dmnCondition = it.dmnCondition.toEntityRef(),
                            sourceBpmnActivityId = it.sourceBpmnActivityId,
                            sourceBpmnActivityEvent = it.sourceBpmnActivityEvent,
                            targetBpmnActivityId = it.targetBpmnActivityId,
                            targetBpmnActivityEvent = it.targetBpmnActivityEvent,
                            durationKpi = it.durationKpi,
                            durationKpiTimeType = it.durationKpiTimeType,
                            countKpi = it.countKpi,
                            countPeriod = it.countPeriod
                        )
                    )
                }
            }
        }

    }

    override fun deployArtifact(artifact: BpmnKpiSettings) {
        println("deployArtifact: ${artifact.toPrettyString()}")

        recordsService.mutate(
            EntityRef.create(AppName.EMODEL, BPMN_KPI_SETTINGS_SOURCE_ID, ""),
            artifact
        )
    }

    private class BpmnKpiSettingsAtts(
        @AttName("record?localId")
        val id: String,

        @AttName
        val kpiType: BpmnKpiType? = null,

        @AttName("record.name!")
        val name: String,

        @AttName("record.enabled!")
        val enabled: Boolean,

        @AttName("record.processRef!")
        val processRef: String,

        @AttName("record.dmnCondition!")
        val dmnCondition: String,

        @AttName("record.sourceBpmnActivityId!")
        val sourceBpmnActivityId: String,
        @AttName("record.sourceBpmnActivityEvent")
        val sourceBpmnActivityEvent: BpmnKpiEventType? = null,

        @AttName("record.targetBpmnActivityId!")
        val targetBpmnActivityId: String,
        @AttName("record.targetBpmnActivityEvent")
        val targetBpmnActivityEvent: BpmnKpiEventType? = null,

        @AttName("record.durationKpi!")
        val durationKpi: String,
        @AttName("record.durationKpiTimeType")
        val durationKpiTimeType: BpmnDurationKpiTimeType? = null,

        @AttName("record.countKpi?num")
        val countKpi: Long? = null,

        @AttName("record.countPeriod!")
        var countPeriod: String
    )
}
