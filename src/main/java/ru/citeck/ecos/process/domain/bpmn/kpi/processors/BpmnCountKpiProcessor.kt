package ru.citeck.ecos.process.domain.bpmn.kpi.processors

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiService
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiValue
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiDefaultStakeholdersFinder
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiSettings
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.txn.lib.TxnContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.lock.EcosAppLockService
import java.time.Duration

@Component
class BpmnCountKpiProcessor(
    private val finder: BpmnKpiDefaultStakeholdersFinder,
    private val recordsService: RecordsService,
    private val bpmnKpiService: BpmnKpiService,
    private val appLockService: EcosAppLockService
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
            bpmnEvent.processRef,
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

            appLockService.doInSync(
                "bpmn-kpi-count-increment", Duration.ofSeconds(10)
            ) {
                TxnContext.doInNewTxn {
                    val foundKpi = bpmnKpiService.queryKpiValues(stakeholder.getRef(), bpmnEvent.processRef)
                        .firstOrNull()

                    if (foundKpi == null) {
                        createNewCountKpiValue(bpmnEvent, stakeholder)
                    } else {
                        incrementKpiValue(foundKpi, stakeholder)
                    }
                }
            }
        }
    }

    private fun createNewCountKpiValue(bpmnEvent: BpmnElementEvent, stakeholder: BpmnKpiSettings) {
        log.trace { "Create count kpiValue for stakeholder: \n${stakeholder.toPrettyString()}" }

        bpmnKpiService.createKpiValue(
            BpmnKpiValue(
                settingsRef = stakeholder.getRef(),
                value = 1,
                processInstanceRef = bpmnEvent.procInstanceRef,
                processRef = bpmnEvent.processRef,
                procDefRef = bpmnEvent.procDefRef,
                document = bpmnEvent.document,
                documentType = bpmnEvent.documentType,
                sourceBpmnActivityId = null,
                targetBpmnActivityId = bpmnEvent.activityId
            )
        )
    }

    private fun incrementKpiValue(kpi: EntityRef, stakeholder: BpmnKpiSettings) {
        val currentKpiValue = recordsService.getAtt(kpi, "value?num").asLong()
        val newValue = currentKpiValue.inc()

        val updated = RecordAtts(kpi)
        updated["value"] = newValue

        log.trace {
            "Update count kpiValue: $currentKpiValue -> $newValue for stakeholder: " +
                "\n${stakeholder.toPrettyString()}"
        }

        recordsService.mutate(updated)
    }
}
