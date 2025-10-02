package ru.citeck.ecos.process.domain.procdef.service

import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

interface ProcDefService {

    fun uploadProcDef(processDef: NewProcessDefDto): ProcDefDto

    fun uploadProcDefDraft(processDef: NewProcessDefDto): ProcDefDto

    fun uploadNewRev(dto: ProcDefWithDataDto, comment: String = ""): ProcDefDto

    fun uploadNewDraftRev(dto: ProcDefWithDataDto, comment: String = "", forceMajorVersion: Boolean = false): ProcDefDto

    fun findAllWithData(workspaces: List<String>, predicate: Predicate?, max: Int, skip: Int): List<ProcDefWithDataDto>

    fun findAll(workspaces: List<String>, predicate: Predicate, max: Int, skip: Int): List<ProcDefDto>

    fun getCount(workspaces: List<String>): Long

    fun getCount(workspaces: List<String>, predicate: Predicate): Long

    fun getCacheKey(): String

    fun getProcessDefRev(procType: String, procDefRevId: UUID): ProcDefRevDto?

    fun saveProcessDefRevDeploymentId(procDefRevId: UUID, deploymentId: String)

    fun getProcessDefRevs(procDefRevIds: List<UUID>): List<ProcDefRevDto>

    fun getProcessDefRevs(ref: ProcDefRef): List<ProcDefRevDto>

    fun getProcessDefRevByDeploymentId(deploymentId: String): ProcDefRevDto?

    fun getProcessDefRevByDeploymentIds(deploymentIds: List<String>): List<ProcDefRevDto>

    /**
     * return first enabled process definition by ecosTypeRef hierarchy
     */
    fun findProcDef(
        procType: String,
        workspace: String,
        ecosTypeRef: EntityRef?,
        alfTypes: List<String>?
    ): ProcDefRevDto?

    fun getProcessDefById(id: ProcDefRef): ProcDefWithDataDto?

    fun delete(ref: ProcDefRef)

    fun listenChanges(type: String, action: (ProcDefDto) -> Unit)
}
