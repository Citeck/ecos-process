package ru.citeck.ecos.process.domain.procdef.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.common.entity.EntityUuid;
import ru.citeck.ecos.process.domain.procdef.entity.ProcDefRevEntity;

@Repository
public interface ProcDefRevRepository extends MongoRepository<ProcDefRevEntity, EntityUuid> {

}
