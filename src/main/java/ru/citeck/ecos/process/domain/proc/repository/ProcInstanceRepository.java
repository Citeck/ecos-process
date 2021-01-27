package ru.citeck.ecos.process.domain.proc.repository;

import ru.citeck.ecos.process.domain.common.repo.EntityUuid;
import ru.citeck.ecos.process.domain.proc.entity.ProcessInstanceEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcInstanceRepository extends MongoRepository<ProcessInstanceEntity, EntityUuid> {
}
