package ru.citeck.ecos.process.domain.procdef.repo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;

@Repository
public interface ProcDefRevRepository extends MongoRepository<ProcDefRevEntity, EntityUuid> {

}
