package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.ProcessDefinition;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;

import java.util.Set;
import java.util.UUID;

@Repository
public interface ProcessDefinitionRevisionRepository extends MongoRepository<ProcessDefinitionRevision, UUID> {

    Set<ProcessDefinitionRevision> findProcessDefinitionRevisionsByProcessDefinitionId(String processDefinitionId);

}
