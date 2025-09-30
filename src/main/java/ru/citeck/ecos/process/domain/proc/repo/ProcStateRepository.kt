package ru.citeck.ecos.process.domain.proc.repo

import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity

interface ProcStateRepository {

    fun findById(id: EntityUuid): ProcessStateEntity?

    fun save(entity: ProcessStateEntity): ProcessStateEntity

    fun findFirstByProcDefRevIn(procDefRev: List<ProcDefRevEntity>): ProcessStateEntity?
}
