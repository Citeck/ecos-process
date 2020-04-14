package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.TimerEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimerRepository extends MongoRepository<TimerEntity, EntityUuid> {
    Optional<TimerEntity> findFirstByIdTntAndActiveAndTriggerTimeBefore(Integer tenant,
                                                                        boolean active,
                                                                        Instant triggerTime);

    Optional<TimerEntity> findById_Id(UUID id);
}
