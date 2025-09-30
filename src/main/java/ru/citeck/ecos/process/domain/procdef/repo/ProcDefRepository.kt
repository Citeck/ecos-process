package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.time.Instant

interface ProcDefRepository {

    fun delete(entity: ProcDefEntity)

    fun deleteAll()

    fun save(entity: ProcDefEntity): ProcDefEntity

    fun findAll(predicate: Predicate, pageable: Pageable): Page<ProcDefEntity>

    fun findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(tenant: Int, type: String, ecosTypeRef: String): ProcDefEntity?

    fun findFirstByIdTntAndProcTypeAndExtId(tenant: Int, type: String, extId: String): ProcDefEntity?

    fun findAllByIdTnt(tenant: Int, pageable: Pageable): List<ProcDefEntity>

    fun getCount(predicate: Predicate): Long

    fun getCount(tenant: Int): Long

    fun getLastModifiedDate(tenant: Int): Instant

    fun findOneByIdTntAndProcTypeAndExtId(tenant: Int, procType: String, extId: String): ProcDefEntity?

    fun findFirstByIdTntAndProcTypeAndAlfType(tenant: Int, type: String, alfType: String): ProcDefEntity?
}
