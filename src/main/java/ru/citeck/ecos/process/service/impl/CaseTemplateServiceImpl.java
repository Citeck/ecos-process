package ru.citeck.ecos.process.service.impl;

import com.google.common.collect.Sets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.repository.CaseTemplateRepository;
import ru.citeck.ecos.process.service.CaseTemplateService;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CaseTemplateServiceImpl implements CaseTemplateService {

    private final CaseTemplateRepository repository;

    @Override
    public Set<CaseTemplateEntity> getAll(Set<String> ids) {
        Iterable<CaseTemplateEntity> iterable = repository.findAllById(ids);
        return Sets.newHashSet(iterable);
    }

    @Override
    public Set<CaseTemplateEntity> getAll() {
        return new HashSet<>(repository.findAll());
    }

    @Override
    public CaseTemplateEntity get(String id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Case template not found"));
    }

    @Override
    public CaseTemplateEntity save(CaseTemplateEntity entity) {
        entity.setId(null);
        return repository.save(entity);
    }
}
