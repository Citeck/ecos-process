package ru.citeck.ecos.process.domain.bpmn.kpi.processors

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiService
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiValue
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiDefaultStakeholdersFinder
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts

@Component
class BpmnCountKpiProcessor(
    private val finder: BpmnKpiDefaultStakeholdersFinder,
    private val recordsService: RecordsService,
    private val bpmnKpiService: BpmnKpiService
) : BpmnKpiProcessor {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun processStartEvent(bpmnEvent: BpmnElementEvent) {
        saveCountKpi(bpmnEvent, BpmnKpiEventType.START)
    }

    override fun processEndEvent(bpmnEvent: BpmnElementEvent) {
        saveCountKpi(bpmnEvent, BpmnKpiEventType.END)
    }

    private fun saveCountKpi(bpmnEvent: BpmnElementEvent, kpiEventType: BpmnKpiEventType) {
        val stakeholders = finder.searchStakeholders(
            bpmnEvent.processId,
            bpmnEvent.document,
            bpmnEvent.activityId,
            kpiEventType,
            BpmnKpiType.COUNT
        )

        stakeholders.forEach { stakeholder ->
            check(stakeholder.getRef().isNotEmpty()) {
                "Stakeholder ref is empty: ${stakeholder.toPrettyString()}"
            }

            log.trace {
                "Found stakeholder: \n${stakeholder.toPrettyString()} " +
                    "\nfor bpmnEvent: \n${bpmnEvent.toPrettyString()}"
            }

            val foundKpi = bpmnKpiService.queryKpiValues(stakeholder.getRef(), bpmnEvent.processId)
                .firstOrNull()

            if (foundKpi == null) {
                log.trace { "Create count kpiValue for stakeholder: \n${stakeholder.toPrettyString()}" }

                bpmnKpiService.createKpiValue(
                    BpmnKpiValue(
                        settingsRef = stakeholder.getRef(),
                        value = 1,
                        processInstanceId = bpmnEvent.procInstanceId,
                        processId = bpmnEvent.processId,
                        document = bpmnEvent.document,
                        documentType = bpmnEvent.documentType,
                        sourceBpmnActivityId = null,
                        targetBpmnActivityId = bpmnEvent.activityId
                    )
                )
            } else {
                // TODO: fix concurrent number inc
                val currentKpiValue = recordsService.getAtt(foundKpi, "value?num").asLong()
                val newValue = currentKpiValue.inc()

                val updated = RecordAtts(foundKpi)
                updated["value"] = newValue

                log.trace {
                    "Update count kpiValue: $currentKpiValue -> $newValue for stakeholder: " +
                        "\n${stakeholder.toPrettyString()}"
                }

                recordsService.mutate(updated)
            }
        }
    }
}
