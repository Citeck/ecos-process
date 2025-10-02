package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import ru.citeck.ecos.records2.predicate.model.Predicate
import java.time.Instant

interface ProcDefRepository {

    fun delete(entity: ProcDefEntity)

    fun deleteAll()

    fun save(entity: ProcDefEntity): ProcDefEntity

    fun findAll(workspaces: List<String>, predicate: Predicate, pageable: Pageable): Page<ProcDefEntity>

    fun findFirstEnabledByEcosType(workspace: String, type: String, ecosTypeRef: String): ProcDefEntity?

    fun findByIdInWs(workspace: String, type: String, extId: String): ProcDefEntity?

    fun getCount(workspaces: List<String>, predicate: Predicate): Long

    fun getCount(workspaces: List<String>): Long

    fun getLastModifiedDate(): Instant

    fun findFirstByProcTypeAndAlfType(workspace: String, type: String, alfType: String): ProcDefEntity?
}
