package ru.citeck.ecos.process.common

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.model.lib.workspace.convertToIdInWsSafe
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.AttributePredicate
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records2.predicate.model.ValuePredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

abstract class EcosDataAbstractAdapter<T : Any>(
    val recordsService: RecordsService,
    val sourceId: String,
    val attsMapping: Map<String, String>,
    val attsType: KClass<T>,
    val workspaceService: WorkspaceService? = null
) {
    companion object {
        const val ATT_ID = "id"
    }

    private val valuePredMappings = ConcurrentHashMap<String, (ValuePredicate) -> Predicate>()

    protected fun registerValuePredMapping(attribute: String, value: (ValuePredicate) -> Predicate) {
        valuePredMappings[attribute] = value
    }

    protected fun getByIdRaw(id: EntityUuid): T? {
        return getByRefRaw(EntityRef.create(sourceId, id.id.toString()))
    }

    protected fun getByRefRaw(ref: EntityRef): T? {
        val idInWs = workspaceService.convertToIdInWsSafe(ref.getLocalId())
        return findAllRaw(
            workspaces = if (idInWs.workspace.isNotBlank()) listOf(idInWs.workspace) else emptyList(),
            predicate = Predicates.eq(ATT_ID, idInWs.id),
            skipCount = 0,
            maxItems = 1
        ).getRecords().firstOrNull()
    }

    protected fun findAllRaw(workspaces: List<String>, predicate: Predicate, pageable: Pageable): Page<T> {
        val (skipCount, maxItems) = if (pageable.isPaged) {
            (pageable.pageSize * pageable.pageNumber) to pageable.pageSize
        } else {
            0 to 50_000
        }
        val sortBy = if (pageable.sort.isSorted) {
            pageable.sort.mapTo(ArrayList()) { SortBy(it.property, it.isAscending) }
        } else {
            emptyList()
        }
        val res = findAllRaw(workspaces, predicate, skipCount, maxItems, sortBy)
        return PageImpl(
            res.getRecords(),
            pageable,
            res.getTotalCount()
        )
    }

    protected fun findAllRaw(predicate: Predicate, pageable: Pageable): Page<T> {
        return findAllRaw(emptyList(), predicate, pageable)
    }

    protected fun findAllRaw(
        workspace: String,
        predicate: Predicate,
        skipCount: Int,
        maxItems: Int,
        sortBy: List<SortBy> = emptyList()
    ): RecsQueryRes<T>  {
        val workspaces = if (workspace.isBlank() || workspace == ModelUtils.DEFAULT_WORKSPACE_ID) {
            listOf("")
        } else {
            listOf(workspace)
        }
        return findAllRaw(workspaces, predicate, skipCount, maxItems, sortBy)
    }

    protected fun findAllRaw(
        workspaces: List<String>,
        predicate: Predicate,
        skipCount: Int,
        maxItems: Int,
        sortBy: List<SortBy> = emptyList()
    ): RecsQueryRes<T>  {
        return findAllRaw(
            workspaces,
            predicate,
            skipCount,
            maxItems,
            sortBy,
            attsType
        )
    }

    protected fun <A: Any> findAllRaw(
        workspaces: List<String>,
        predicate: Predicate,
        skipCount: Int,
        maxItems: Int,
        sortBy: List<SortBy> = emptyList(),
        attsType: KClass<A>
    ): RecsQueryRes<A> {

        val resPred = PredicateUtils.mapAttributePredicates(predicate, { pred ->
            var result: Predicate = pred
            if (result is ValuePredicate) {
                val valueMapping = valuePredMappings[result.getAttribute()]
                if (valueMapping != null) {
                    result = valueMapping.invoke(result)
                } else if (result.getAttribute() == RecordConstants.ATT_TYPE) {
                    result = Predicates.alwaysTrue()
                }
            }
            val mappedAtt = attsMapping[pred.getAttribute()]
            if (mappedAtt != null) {
                val newPred = result.copy<AttributePredicate>()
                newPred.setAtt(mappedAtt)
                result = newPred
            }
            result
        }, onlyAnd = false, optimize = true, filterEmptyComposite = false)

        val recsQuery = RecordsQuery.create()
            .withSourceId(sourceId)
            .withQuery(resPred)
            .withMaxItems(maxItems)
            .withSkipCount(skipCount)
            .withWorkspaces(workspaces)
            .withSortBy(
                sortBy.map {
                    SortBy(attsMapping.getOrDefault(it.attribute, it.attribute), it.ascending)
                }.ifEmpty {
                    listOf(SortBy(RecordConstants.ATT_CREATED, true))
                }
            )

        return if (EntityRef::class.isSuperclassOf(attsType)) {
            @Suppress("UNCHECKED_CAST")
            recordsService.query(recsQuery.build()) as RecsQueryRes<A>
        } else {
            recordsService.query(recsQuery.build(), attsType.java)
        }
    }

    fun deleteAll() {
        fun nextChunk(): List<EntityRef> {
            return findAllRaw(
                emptyList(),
                Predicates.alwaysTrue(),
                0,
                100,
                emptyList(),
                EntityRef::class
            ).getRecords()
        }
        var toDelete = nextChunk()
        while (toDelete.isNotEmpty()) {
            recordsService.delete(toDelete)
            toDelete = nextChunk()
        }
    }
}
