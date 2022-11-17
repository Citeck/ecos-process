package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import ru.citeck.ecos.process.domain.common.repo.EntityUuid

@Repository
interface ProcDefRevRepository : MongoRepository<ProcDefRevEntity, EntityUuid> {

    fun findAllByProcessDef(procDef: ProcDefEntity): List<ProcDefRevEntity>

    fun findByDeploymentId(deploymentId: String): ProcDefRevEntity?

    fun queryAllByDeploymentIdIsNotNull(): List<ProcDefRevEntity>
}
