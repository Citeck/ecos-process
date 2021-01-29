package ru.citeck.ecos.process.domain.proc.repo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity

@Repository
interface ProcStateRepository : MongoRepository<ProcessStateEntity, EntityUuid> {

    fun findFirstByProcDefRevIn(procDefRev: List<ProcDefRevEntity>): ProcessStateEntity?
}
