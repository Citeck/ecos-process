package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessDefEntity;

import java.util.Optional;

@Repository
public interface ProcessDefRepository extends MongoRepository<ProcessDefEntity, EntityUuid> {

    Optional<ProcessDefEntity> findFirstByTenantAndProcTypeAndEcosTypeRef(int tenant, String type, String ecosTypeRef);

    Optional<ProcessDefEntity> findFirstByTenantAndProcTypeAndExtId(int tenant, String type, String extId);

    Optional<ProcessDefEntity> findFirstByTenantAndProcTypeAndAlfType(int tenant, String type, String alfType);
}
