package ru.citeck.ecos.process.domain.proc.repo.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity

@Repository
interface MongoProcInstanceRepository : MongoRepository<ProcessInstanceEntity, EntityUuid>
