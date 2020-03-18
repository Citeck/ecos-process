package ru.citeck.ecos.process.service.impl;

import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.eapps.casetemplate.CaseTemplateDTO;
import ru.citeck.ecos.process.eapps.casetemplate.ListenModuleChanges;
import ru.citeck.ecos.process.eapps.casetemplate.aop.TrackChanges;
import ru.citeck.ecos.process.repository.CaseTemplateRepository;
import ru.citeck.ecos.process.service.CaseTemplateService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class CaseTemplateServiceImpl implements CaseTemplateService {

    private final CaseTemplateRepository repository;

    @Override
    public Set<CaseTemplateEntity> getAll(@NonNull Set<String> ids) {
        Iterable<CaseTemplateEntity> iterable = repository.findAllById(ids);
        return Sets.newHashSet(iterable);
    }

    @Override
    public Set<CaseTemplateEntity> getAll() {
        return new HashSet<>(repository.findAll());
    }

    @Override
    public CaseTemplateEntity get(@NonNull String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Case template not found"));
    }

    @TrackChanges
    @Override
    public CaseTemplateEntity save(@NonNull CaseTemplateEntity entity) {
        entity.setId(null);
        return repository.save(entity);
    }

    @Override
    public void delete(@NonNull String id) {
        repository.deleteById(id);
    }
}
