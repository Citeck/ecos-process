package ru.citeck.ecos.process.domain.procdef.repo.mongo

import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.querydsl.QuerydslPredicateExecutor
import org.springframework.stereotype.Repository
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity

@Repository
interface MongoProcDefRepo : MongoRepository<ProcDefEntity, EntityUuid>, QuerydslPredicateExecutor<ProcDefEntity> {

    fun findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(tenant: Int, type: String, ecosTypeRef: String): ProcDefEntity?

    fun findFirstByIdTntAndProcTypeAndExtId(tenant: Int, type: String, extId: String): ProcDefEntity?

    fun findAllByIdTnt(tenant: Int, pageable: Pageable): List<ProcDefEntity>

    @Query(value = "{ 'id.tnt' : ?0 }", count = true)
    fun getCount(tenant: Int): Long

    @Query(value = "{ 'id.tnt' : ?0 }", fields = "{ _id: 0, modified: 1 }")
    fun getModifiedDate(tenant: Int, pageable: Pageable): List<ProcDefEntity>

    fun findOneByIdTntAndProcTypeAndExtId(tenant: Int, procType: String, extId: String): ProcDefEntity?

    fun findFirstByIdTntAndProcTypeAndAlfType(tenant: Int, type: String, alfType: String): ProcDefEntity?
}
