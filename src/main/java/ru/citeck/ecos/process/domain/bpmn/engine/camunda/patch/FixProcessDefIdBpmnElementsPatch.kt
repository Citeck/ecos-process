package ru.citeck.ecos.process.domain.bpmn.engine.camunda.patch

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.common.toPrettyString
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessLatestRecords
import ru.citeck.ecos.process.domain.bpmn.elements.api.records.BpmnProcessElementsProxyDao.Companion.BPMN_ELEMENTS_SOURCE_ID
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.patch.PatchExecutionState
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatchDependsOnApps
import ru.citeck.ecos.webapp.lib.patch.executor.bean.StatefulEcosPatch

private const val BATCH_SIZE = 100

@Component
@EcosPatchDependsOnApps(AppName.EMODEL)
@EcosPatch("fix-process-def-id-bpmn-elements", "2023-12-26T00:00:00Z")
class FixProcessDefIdBpmnElementsPatch(
    private val recordsService: RecordsService,
    private val procDefRefResolver: CacheableProcDefRefResolver
) : StatefulEcosPatch<PatchState> {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun execute(state: PatchState): PatchExecutionState<PatchState> {
        val elementsToFix = queryForFix(state)

        if (elementsToFix.isEmpty()) {
            log.info { "No elements to fix. State: \n${state.toPrettyString()}" }
            return PatchExecutionState(state, true)
        }

        log.info { "Process ${elementsToFix.size} elements." }

        val notFoundProcDefRefByProcessId = state.notFoundProcDefRef.toMutableSet()

        elementsToFix
            .filter { !it.procDefId.isNullOrBlank() }
            .forEach { element ->
                val procDefRef = procDefRefResolver.getProcessDefRefByProcessId(element.procDefId!!).toEntityRef()
                if (procDefRef.getLocalId().isBlank()) {
                    notFoundProcDefRefByProcessId.add(element.procDefId!!)
                    return@forEach
                }

                val atts = RecordAtts(element.id)
                atts["__disableEvents"] = true
                atts["procDefId"] = procDefRef.getLocalId()
                atts["procDefRef"] = procDefRef

                recordsService.mutate(atts)
            }

        val patchedElements = state.patchedElements + elementsToFix.size

        log.info { "Patched $patchedElements bpmn elements" }

        return PatchExecutionState(
            PatchState(
                patchedElements = patchedElements,
                notFoundProcDefRef = notFoundProcDefRefByProcessId
            ),
            false
        )
    }

    private fun queryForFix(state: PatchState): List<BpmnElementData> {
        val notProcDefEqPredicates = if (state.notFoundProcDefRef.isEmpty()) {
            listOf(Predicates.alwaysTrue())
        } else {
            state.notFoundProcDefRef
                .map {
                    Predicates.not(Predicates.eq("procDefId", it))
                }
        }

        val found = recordsService.query(
            RecordsQuery.create {
                withSourceId("${AppName.EPROC}/$BPMN_ELEMENTS_SOURCE_ID")
                withQuery(
                    Predicates.and(
                        Predicates.eq("engine", "camunda"),
                        Predicates.empty("processId"),
                        Predicates.empty("procDefRef"),
                        Predicates.and(
                            *notProcDefEqPredicates.toTypedArray()
                        )
                    )
                )
                withMaxItems(BATCH_SIZE)
            },
            BpmnElementData::class.java
        )

        log.info { "Found total count: ${found.getTotalCount()}" }

        return found.getRecords()
    }

    private class BpmnElementData(
        @AttName("?id")
        var id: EntityRef,

        var procDefId: String? = null,
    )
}

data class PatchState(
    var patchedElements: Long = 0,
    var notFoundProcDefRef: Set<String> = emptySet()
)

@Component
class CacheableProcDefRefResolver(
    private val recordsService: RecordsService
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    @Cacheable(cacheNames = ["processDefRefByProcessId"])
    fun getProcessDefRefByProcessId(processId: String): String {
        log.info { "Get process def ref by process id: $processId" }

        return recordsService.getAtt(
            EntityRef.create(AppName.EPROC, BpmnProcessLatestRecords.ID, processId),
            "definition?id"
        ).asText()
    }
}
