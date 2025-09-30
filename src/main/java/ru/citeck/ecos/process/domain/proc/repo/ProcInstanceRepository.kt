package ru.citeck.ecos.process.domain.proc.repo

import ru.citeck.ecos.process.domain.common.repo.EntityUuid

interface ProcInstanceRepository {

    fun findById(id: EntityUuid): ProcessInstanceEntity?

    fun save(entity: ProcessInstanceEntity): ProcessInstanceEntity
}
