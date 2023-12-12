package ru.citeck.ecos.process.domain.bpmn.kpi.processors

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsDao
import ru.citeck.ecos.process.domain.bpmn.kpi.*
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiDefaultStakeholdersFinder
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiSettings
import ru.citeck.ecos.records2.predicate.PredicateService
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.Predicates.eq
import ru.citeck.ecos.records2.predicate.model.Predicates.notEmpty
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import java.time.Duration
import java.time.Instant

@Component
class BpmnDurationKpiProcessor(
    private val finder: BpmnKpiDefaultStakeholdersFinder,
    private val recordsService: RecordsService,
    private val bpmnKpiService: BpmnKpiService
) : BpmnKpiProcessor {

    companion object {
        private const val WK_SCHEDULE_SOURCE_ID = "emodel/working-schedule-action"

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
                    withSourceId(BpmnProcessElementsDao.BPMN_ELEMENTS_SOURCE_ID)
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
                        processId = bpmnEvent.processId
                    )
                )
            }
        }
    }

    private fun getWorkingTimeDiff(start: Instant, end: Instant): Duration {
        val diffDuration = recordsService.queryOne(
            RecordsQuery.create {
                withSourceId(WK_SCHEDULE_SOURCE_ID)
                withQuery(
                    """
                        {
                          "type": "get-working-time",
                          "config": {
                            "from": "$start",
                            "to": "$end"
                          },
                          "query": {}
                        }
                    """.trimIndent()
                )
            },
            "data"
        ).asText()

        log.trace { "Getting working time between $start and $end: $diffDuration" }

        return Duration.parse(diffDuration)
    }

    private data class ElementInfo(
        var created: Instant? = null,
        var completed: Instant? = null
    )
}
