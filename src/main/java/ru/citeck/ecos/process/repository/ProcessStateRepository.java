package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessStateEntity;

@Repository
public interface ProcessStateRepository extends MongoRepository<ProcessStateEntity, EntityUuid> {

}
