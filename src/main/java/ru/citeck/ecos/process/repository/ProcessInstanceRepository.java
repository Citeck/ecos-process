package ru.citeck.ecos.process.repository;

import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessInstanceEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessInstanceRepository extends MongoRepository<ProcessInstanceEntity, EntityUuid> {
}
