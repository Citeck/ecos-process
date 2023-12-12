package ru.citeck.ecos.process.domain.bpmn.kpi.eapp

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.events2.EventsService
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnDurationKpiTimeType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.config.BpmnKpiSettingsDaoConfig
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiSettings
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Consumer

@Component
class BpmnKpiSettingsArtifactHandler(
    private val recordsService: RecordsService,
    private val eventsService: EventsService
) : EcosArtifactHandler<BpmnKpiSettings> {

    override fun deleteArtifact(artifactId: String) {
        recordsService.delete(EntityRef.create(AppName.EPROC, BpmnKpiSettingsDaoConfig.SOURCE_ID, artifactId))
    }

    override fun getArtifactType(): String {
        return "process/bpmn-kpi-settings"
    }

    override fun listenChanges(listener: Consumer<BpmnKpiSettings>) {
        eventsService.addListener<BpmnKpiSettingsAtts> {
            withDataClass(BpmnKpiSettingsAtts::class.java)
            withFilter(Predicates.eq("typeDef.id", "bpmn-kpi-settings"))
            withLocal(true)
            withAction {
                listener.accept(
                    BpmnKpiSettings(
                        id = it.id,
                        name = it.name,
                        enabled = it.enabled,
                        processRef = it.processRef,
                        dmnCondition = it.dmnCondition,
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

    override fun deployArtifact(artifact: BpmnKpiSettings) {
        recordsService.mutate(
            EntityRef.create(AppName.EPROC, BpmnKpiSettingsDaoConfig.SOURCE_ID, ""),
            artifact
        )
    }

    private class BpmnKpiSettingsAtts(
        @AttName("record?localId")
        val id: String,

        @AttName("record.name!")
        val name: String,

        @AttName("record.enabled!")
        val enabled: Boolean,

        @AttName("record.processRef?id")
        val processRef: EntityRef,

        @AttName("record.dmnCondition")
        val dmnCondition: EntityRef,

        @AttName("record.sourceBpmnActivityId!")
        val sourceBpmnActivityId: String,
        @AttName("record.sourceBpmnActivityEvent")
        val sourceBpmnActivityEvent: BpmnKpiEventType? = null,

        @AttName("record.targetBpmnActivityId")
        val targetBpmnActivityId: String,
        @AttName("record.targetBpmnActivityEvent")
        val targetBpmnActivityEvent: BpmnKpiEventType? = null,

        @AttName("record.durationKpi!")
        val durationKpi: String,
        @AttName("record.durationKpiTimeType")
        val durationKpiTimeType: BpmnDurationKpiTimeType? = null,

        @AttName("record.countKpi?num")
        val countKpi: Long,

        @AttName("record.countPeriod!")
        var countPeriod: String
    )
}
