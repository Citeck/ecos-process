package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.CaseTemplateDto;

import java.util.Set;
import java.util.function.Consumer;

public interface CaseTemplateService {
    Set<CaseTemplateDto> getAll(Set<String> ids);

    Set<CaseTemplateDto> getAll();

    CaseTemplateDto get(String id);

    CaseTemplateDto save(CaseTemplateDto entity);

    void delete(String id);

    void setChangesListener(Consumer<CaseTemplateDto> changesListener);
}
