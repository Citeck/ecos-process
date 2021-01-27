package ru.citeck.ecos.process.domain.procdef.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;
import ru.citeck.ecos.process.domain.procdef.entity.ProcDefEntity;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProcDefRepository
    extends MongoRepository<ProcDefEntity, EntityUuid>, QuerydslPredicateExecutor<ProcDefEntity> {

    Optional<ProcDefEntity> findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(int tenant, String type, String ecosTypeRef);

    Optional<ProcDefEntity> findFirstByIdTntAndProcTypeAndExtId(int tenant, String type, String extId);

    List<ProcDefEntity> findAllByIdTnt(int tenant, Pageable pageable);

    @Query(value = "{ 'id.tnt' : ?0 }", count = true)
    long getCount(int tenant);

    @Query(value = "{ 'extId' : ?0 }", count = true)
    long getCount(int tenant, String extId);

    List<ProcDefEntity> findAllByIdTntAndExtIdLike(int tenant, String extId, Pageable pageable);

    Optional<ProcDefEntity> findFirstByIdTntAndProcTypeAndAlfType(int tenant, String type, String alfType);
}
