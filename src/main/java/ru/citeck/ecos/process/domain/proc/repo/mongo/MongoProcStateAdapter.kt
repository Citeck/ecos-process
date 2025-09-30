package ru.citeck.ecos.process.domain.proc.repo.mongo

import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository
import ru.citeck.ecos.process.domain.proc.repo.ProcessStateEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import java.util.*

class MongoProcStateAdapter(
    val impl: MongoProcStateRepository
) : ProcStateRepository {

    override fun findById(id: EntityUuid): ProcessStateEntity? {
        return impl.findById(id).orElse(null)
    }

    override fun save(entity: ProcessStateEntity): ProcessStateEntity {
        if (entity.id == null) {
            entity.id = EntityUuid(0, UUID.randomUUID())
        }
        return impl.save(entity)
    }

    override fun findFirstByProcDefRevIn(procDefRev: List<ProcDefRevEntity>): ProcessStateEntity? {
        return impl.findFirstByProcDefRevIn(procDefRev)
    }
}
