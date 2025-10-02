package ru.citeck.ecos.process.domain.procdef.repo.mongo

import com.querydsl.core.types.dsl.BooleanExpression
import lombok.Data
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_BPMN_SECTION
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.QProcDefEntity
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.time.Instant
import java.util.*

class MongoProcDefRepoAdapter(
    private val impl: MongoProcDefRepo
) : ProcDefRepository {

    override fun delete(entity: ProcDefEntity) {
        return impl.delete(entity)
    }

    override fun deleteAll() {
        impl.deleteAll()
    }

    override fun save(entity: ProcDefEntity): ProcDefEntity {
        if (entity.id == null) {
            entity.id = EntityUuid(0, UUID.randomUUID())
        }
        return impl.save(entity)
    }

    override fun findAll(workspaces: List<String>, predicate: Predicate, pageable: Pageable): Page<ProcDefEntity> {
        return impl.findAll(predicateToQuery(predicate), pageable)
    }

    override fun findFirstEnabledByEcosType(workspace: String, type: String, ecosTypeRef: String): ProcDefEntity? {
        return impl.findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(0, type, ecosTypeRef)
    }

    override fun findByIdInWs(workspace: String, type: String, extId: String): ProcDefEntity? {
        return impl.findFirstByIdTntAndProcTypeAndExtId(0, type, extId)
    }

    override fun getCount(workspaces: List<String>, predicate: Predicate): Long {
        return impl.count(predicateToQuery(predicate))
    }

    override fun getCount(workspaces: List<String>): Long {
        return impl.getCount(0)
    }

    override fun getLastModifiedDate(): Instant {
        val page = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("modified")))
        val modified = impl.getModifiedDate(0, page)
        return if (modified.isEmpty()) {
            Instant.EPOCH
        } else {
            Instant.ofEpochMilli(modified[0].modified?.toEpochMilli() ?: 0L)
        }
    }

    override fun findFirstByProcTypeAndAlfType(workspace: String, type: String, alfType: String): ProcDefEntity? {
        return impl.findFirstByIdTntAndProcTypeAndAlfType(0, type, alfType)
    }

    private fun predicateToQuery(predicate: Predicate): BooleanExpression {

        val predQuery = PredicateUtils.convertToDto(predicate, PredicateQuery::class.java)
        val entity = QProcDefEntity.procDefEntity
        var query = entity.id.tnt.eq(0)

        if (StringUtils.isNotBlank(predQuery.procType)) {
            query = query.and(QProcDefEntity.procDefEntity.procType.eq(predQuery.procType))
        }
        if (StringUtils.isNotBlank(predQuery.moduleId)) {
            query = query.and(QProcDefEntity.procDefEntity.extId.containsIgnoreCase(predQuery.moduleId))
        }
        if (StringUtils.isNotBlank(predQuery.name)) {
            query = query.and(QProcDefEntity.procDefEntity.name.containsIgnoreCase(predQuery.name))
        }
        if (StringUtils.isNotBlank(predQuery.sectionRef)) {
            val sectionEntity = QProcDefEntity.procDefEntity.sectionRef
            query = if (predQuery.sectionRef.equals(DEFAULT_BPMN_SECTION)) {
                query.and(sectionEntity.eq(DEFAULT_BPMN_SECTION).or(sectionEntity.isEmpty).or(sectionEntity.isNull))
            } else {
                query.and(QProcDefEntity.procDefEntity.sectionRef.eq(predQuery.sectionRef))
            }
        }
        return query
    }

    @Data
    class PredicateQuery {
        val moduleId: String? = null
        val procType: String? = null
        val sectionRef: String? = null
        val name: String? = null
    }
}
