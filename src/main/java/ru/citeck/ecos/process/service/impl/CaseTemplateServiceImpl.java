package ru.citeck.ecos.process.service.impl;

import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.process.domain.CaseTemplate;
import ru.citeck.ecos.process.service.dto.CaseTemplateDto;
import ru.citeck.ecos.process.aop.eapps.listener.TrackChanges;
import ru.citeck.ecos.process.service.mapper.CaseTemplateMapper;
import ru.citeck.ecos.process.repository.CaseTemplateRepository;
import ru.citeck.ecos.process.service.CaseTemplateService;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CaseTemplateServiceImpl implements CaseTemplateService {

    private final CaseTemplateRepository repository;
    private final CaseTemplateMapper mapper;

    @Override
    public Set<CaseTemplateDto> getAll(@NonNull Set<String> ids) {
        Iterable<CaseTemplate> iterable = repository.findAllById(ids);
        return Sets.newHashSet(iterable).stream()
            .map(mapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<CaseTemplateDto> getAll() {
        return new HashSet<>(repository.findAll()).stream()
            .map(mapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public CaseTemplateDto get(@NonNull String id) {
        Optional<CaseTemplate> optional = repository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("Case template not found by id: " + id);
        }
        return mapper.entityToDto(optional.get());
    }

    @TrackChanges
    @Transactional
    @Override
    public CaseTemplateDto save(@NonNull CaseTemplateDto dto) {
        dto.setId(null);
        CaseTemplate received = mapper.dtoToEntity(dto);
        CaseTemplate persisted = repository.save(received);
        return mapper.entityToDto(persisted);
    }

    @Transactional
    @Override
    public void delete(@NonNull String id) {
        repository.deleteById(id);
    }
}
