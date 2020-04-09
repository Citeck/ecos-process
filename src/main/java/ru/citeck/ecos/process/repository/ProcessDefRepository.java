package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessDefEntity;

import java.util.Optional;

@Repository
public interface ProcessDefRepository extends MongoRepository<ProcessDefEntity, EntityUuid> {

    Optional<ProcessDefEntity> findFirstByProcTypeAndEcosTypeRef(String type, String ecosTypeRef);

    Optional<ProcessDefEntity> findFirstByProcTypeAndExtId(String type, String extId);

    Optional<ProcessDefEntity> findFirstByProcTypeAndAlfType(String type, String alfType);
}
