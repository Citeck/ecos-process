package ru.citeck.ecos.process.domain.bpmn.kpi.processors

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao
import ru.citeck.ecos.process.domain.bpmn.kpi.*
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiDefaultStakeholdersFinder
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiSettings
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.Predicates.eq
import ru.citeck.ecos.records2.predicate.model.Predicates.notEmpty
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.wkgsch.lib.schedule.WorkingScheduleService
import java.time.Duration
import java.time.Instant

@Component
class BpmnDurationKpiProcessor(
    private val finder: BpmnKpiDefaultStakeholdersFinder,
    private val recordsService: RecordsService,
    private val bpmnKpiService: BpmnKpiService,
    private val workingScheduleService: WorkingScheduleService
) : BpmnKpiProcessor {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun processStartEvent(bpmnEvent: BpmnElementEvent) {
        saveStakeholdersKpi(
            bpmnEvent,
            BpmnKpiEventType.START
        ) { stakeholder, foundSourceElement ->
            val targetCompletedTime = bpmnEvent.created
            val sourceTime = if (stakeholder.sourceBpmnActivityEvent == BpmnKpiEventType.START) {
                foundSourceElement.created ?: return@saveStakeholdersKpi null
            } else {
                foundSourceElement.completed ?: return@saveStakeholdersKpi null
            }

            when (stakeholder.durationKpiTimeType) {
                BpmnDurationKpiTimeType.CALENDAR -> {
                    targetCompletedTime.toEpochMilli() - sourceTime.toEpochMilli()
                }

                BpmnDurationKpiTimeType.WORKING -> {
                    getWorkingTimeDiff(sourceTime, targetCompletedTime).toMillis()
                }

                else -> {
                    error(
                        "Unknown time type: ${stakeholder.durationKpiTimeType} " +
                            "of stakeholder: \n${stakeholder.toPrettyString()}"
                    )
                }
            }
        }
    }

    override fun processEndEvent(bpmnEvent: BpmnElementEvent) {
        check(bpmnEvent.completed != null) {
            "Completed date must be not null: \n${Json.mapper.toPrettyString(bpmnEvent)}"
        }

        saveStakeholdersKpi(
            bpmnEvent,
            BpmnKpiEventType.END
        ) { stakeholder, foundSourceElement ->
            val targetCompletedTime = bpmnEvent.completed
            val sourceTime = if (stakeholder.sourceBpmnActivityEvent == BpmnKpiEventType.START) {
                foundSourceElement.created ?: return@saveStakeholdersKpi null
            } else {
                foundSourceElement.completed ?: return@saveStakeholdersKpi null
            }

            when (stakeholder.durationKpiTimeType) {
                BpmnDurationKpiTimeType.CALENDAR -> {
                    targetCompletedTime.toEpochMilli() - sourceTime.toEpochMilli()
                }

                BpmnDurationKpiTimeType.WORKING -> {
                    getWorkingTimeDiff(sourceTime, targetCompletedTime).toMillis()
                }

                else -> {
                    error(
                        "Unknown time type: ${stakeholder.durationKpiTimeType} " +
                            "of stakeholder: \n${stakeholder.toPrettyString()}"
                    )
                }
            }
        }
    }

    private fun saveStakeholdersKpi(
        bpmnEvent: BpmnElementEvent,
        kpiEventType: BpmnKpiEventType,
        calcKpi: (stakeHolder: BpmnKpiSettings, foundSourceElement: ElementInfo) -> Number?
    ) {
        val stakeholders = finder.searchStakeholders(
            bpmnEvent.processId,
            bpmnEvent.document,
            bpmnEvent.activityId,
            kpiEventType,
            BpmnKpiType.DURATION
        )

        stakeholders.forEach { stakeholder ->

            log.trace {
                "Found stakeholder: \n${stakeholder.toPrettyString()} " +
                    "\nfor bpmnEvent: \n${bpmnEvent.toPrettyString()}"
            }

            val elementCompletionPredicate = if (stakeholder.sourceBpmnActivityEvent == BpmnKpiEventType.START) {
                notEmpty("created")
            } else {
                notEmpty("completed")
            }

            val foundSourceElement = recordsService.query(
                RecordsQuery.create {
                    withSourceId(BpmnProcessElementsProxyDao.BPMN_ELEMENTS_SOURCE_ID)
                    withLanguage(PredicateService.LANGUAGE_PREDICATE)
                    withQuery(
                        Predicates.and(
                            eq("procInstanceId", bpmnEvent.procInstanceId),
                            eq("elementDefId", stakeholder.sourceBpmnActivityId),
                            elementCompletionPredicate
                        )
                    )
                },
                ElementInfo::class.java
            ).getRecords().firstOrNull() ?: return@forEach

            calcKpi(stakeholder, foundSourceElement)?.let { kpiValue ->
                log.trace { "Create duration kpiValue: $kpiValue for stakeholder: \n${stakeholder.toPrettyString()}" }

                bpmnKpiService.createKpiValue(
                    BpmnKpiValue(
                        settingsRef = stakeholder.getRef(),
                        value = kpiValue,
                        processInstanceId = bpmnEvent.procInstanceId,
                        processId = bpmnEvent.processId,
                        document = bpmnEvent.document,
                        documentType = bpmnEvent.documentType,
                        sourceBpmnActivityId = foundSourceElement.elementDefId,
                        targetBpmnActivityId = bpmnEvent.activityId
                    )
                )
            }
        }
    }

    private fun getWorkingTimeDiff(start: Instant, end: Instant): Duration {
        val schedule = workingScheduleService.getScheduleById("DEFAULT")
        val duration = schedule.getWorkingTime(start, end)

        log.trace { "Getting working time between $start and $end: $duration" }

        return duration
    }

    private data class ElementInfo(
        var created: Instant? = null,
        var completed: Instant? = null,
        var elementDefId: String? = null
    )
}
