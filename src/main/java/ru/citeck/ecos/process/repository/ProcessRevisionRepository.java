package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.domain.ProcessRevision;

import java.util.Set;
import java.util.UUID;

@Repository
public interface ProcessRevisionRepository extends MongoRepository<ProcessRevision, UUID> {

    Set<ProcessRevision> findProcessRevisionsByProcessId(UUID processId);
}
