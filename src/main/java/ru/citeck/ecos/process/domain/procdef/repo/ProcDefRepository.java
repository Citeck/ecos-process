package ru.citeck.ecos.process.domain.procdef.repo;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;

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

    @Query(value = "{ 'id.tnt' : ?0, 'extId' : ?1 }", count = true)
    long getCount(int tenant, String extId);

    @Query(value = "{ 'id.tnt' : ?0 }", fields = "{ _id: 0, modified: 1 }")
    List<ProcDefEntity> getModifiedDate(int tenant, Pageable pageable);

    List<ProcDefEntity> findAllByIdTntAndExtIdLike(int tenant, String extId, Pageable pageable);

    Optional<ProcDefEntity> findFirstByIdTntAndProcTypeAndAlfType(int tenant, String type, String alfType);
}
