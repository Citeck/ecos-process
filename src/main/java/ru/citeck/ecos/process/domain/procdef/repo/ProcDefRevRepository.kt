package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import org.springframework.stereotype.Repository
import ru.citeck.ecos.process.domain.common.repo.EntityUuid

@Repository
interface ProcDefRevRepository : MongoRepository<ProcDefRevEntity, EntityUuid> {

    /**
     *  Return def rev entity without data, because it is too big memory usage.
     *  If you need get revisions with data, implement method with [Pageable]
     */
    @Query(fields = "{ 'data' : null }")
    fun findAllByProcessDef(processDef: ProcDefEntity): List<ProcDefRevEntity>

    fun findByDeploymentId(deploymentId: String): ProcDefRevEntity?

    fun queryAllByDeploymentIdIsNotNull(pageable: Pageable): Slice<ProcDefRevEntity>
}
