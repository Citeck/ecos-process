package ru.citeck.ecos.process.domain.proc.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;
import ru.citeck.ecos.process.domain.proc.entity.ProcessStateEntity;

@Repository
public interface ProcStateRepository extends MongoRepository<ProcessStateEntity, EntityUuid> {

}
