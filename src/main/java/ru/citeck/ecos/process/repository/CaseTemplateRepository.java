package ru.citeck.ecos.process.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;

@Repository
public interface CaseTemplateRepository extends MongoRepository<CaseTemplateEntity, String> {
}
