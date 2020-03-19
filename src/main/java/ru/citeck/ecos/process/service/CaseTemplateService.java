package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.service.dto.CaseTemplateDto;

import java.util.Set;

public interface CaseTemplateService {
    Set<CaseTemplateDto> getAll(Set<String> ids);

    Set<CaseTemplateDto> getAll();

    CaseTemplateDto get(String id);

    CaseTemplateDto save(CaseTemplateDto entity);

    void delete(String id);
}
