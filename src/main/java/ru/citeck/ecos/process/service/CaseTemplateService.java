package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.domain.CaseTemplateEntity;

import java.util.Arrays;
import java.util.Set;

public interface CaseTemplateService {
    Set<CaseTemplateEntity> getAll(Set<String> ids);

    Set<CaseTemplateEntity> getAll();

    CaseTemplateEntity get(String id);

    CaseTemplateEntity save(CaseTemplateEntity entity);
}
