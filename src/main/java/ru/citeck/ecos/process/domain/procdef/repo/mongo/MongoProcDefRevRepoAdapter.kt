package ru.citeck.ecos.process.domain.procdef.repo.mongo

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import java.util.*

class MongoProcDefRevRepoAdapter(
    val impl: MongoProcDefRevRepo
) : ProcDefRevRepository {

    override fun save(entity: ProcDefRevEntity): ProcDefRevEntity {
        if (entity.id == null) {
            entity.id = EntityUuid(0, UUID.randomUUID())
        }
        return impl.save(entity)
    }

    override fun findById(id: EntityUuid): ProcDefRevEntity? {
        return impl.findById(id).orElse(null)
    }

    override fun findAllById(ids: Iterable<EntityUuid>): List<ProcDefRevEntity> {
        return impl.findAllById(ids)
    }

    override fun findAllByProcessDef(processDef: ProcDefEntity): List<ProcDefRevEntity> {
        return impl.findAllByProcessDef(processDef)
    }

    override fun findByDeploymentId(deploymentId: String): ProcDefRevEntity? {
        return impl.findByDeploymentId(deploymentId)
    }

    override fun findByDeploymentIdIsIn(deploymentIds: List<String>): List<ProcDefRevEntity> {
        return impl.findByDeploymentIdIsIn(deploymentIds)
    }

    override fun queryAllByDeploymentIdIsNotNull(pageable: Pageable): Slice<ProcDefRevEntity> {
        return impl.queryAllByDeploymentIdIsNotNull(pageable)
    }

    override fun deleteAll(entities: List<ProcDefRevEntity>) {
        impl.deleteAll(entities)
    }

    override fun deleteAll() {
        impl.deleteAll()
    }
}
