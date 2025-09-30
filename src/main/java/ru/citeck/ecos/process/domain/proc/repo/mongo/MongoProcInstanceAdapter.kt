package ru.citeck.ecos.process.domain.proc.repo.mongo

import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity
import java.util.*

class MongoProcInstanceAdapter(
    val impl: MongoProcInstanceRepository
) : ProcInstanceRepository {

    override fun findById(id: EntityUuid): ProcessInstanceEntity? {
        return impl.findById(id).orElse(null)
    }

    override fun save(entity: ProcessInstanceEntity): ProcessInstanceEntity {
        if (entity.id == null) {
            entity.id = EntityUuid(0, UUID.randomUUID())
        }
        return impl.save(entity)
    }
}
