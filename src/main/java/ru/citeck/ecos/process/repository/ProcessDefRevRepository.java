package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessDefRevEntity;

@Repository
public interface ProcessDefRevRepository extends MongoRepository<ProcessDefRevEntity, EntityUuid> {

}
