package ru.citeck.ecos.process.service.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.repository.ProcessDefinitionRevisionRepository;
import ru.citeck.ecos.process.service.ProcessDefinitionRevisionService;
import ru.citeck.ecos.process.service.dto.ProcessDefinitionRevisionDto;
import ru.citeck.ecos.process.service.mapper.ProcessDefinitionRevisionMapper;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessDefinitionRevisionServiceImpl implements ProcessDefinitionRevisionService {

    private final ProcessDefinitionRevisionRepository processDefinitionRevisionRepository;
    private final ProcessDefinitionRevisionMapper mapper;

    @Override
    public Set<ProcessDefinitionRevisionDto> getAll() {
        return processDefinitionRevisionRepository.findAll().stream()
            .map(mapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public ProcessDefinitionRevisionDto getById(@NonNull UUID id) {
        Optional<ProcessDefinitionRevision> optional = processDefinitionRevisionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("Process definition not found with id: " + id);
        }
        return mapper.entityToDto(optional.get());
    }

    @Override
    public Set<ProcessDefinitionRevisionDto> getAllByProcessDefinitionId(String processDefinitionId) {
        return processDefinitionRevisionRepository
            .findProcessDefinitionRevisionsByProcessDefinitionId(processDefinitionId)
            .stream()
            .map(mapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Transactional
    @Override
    public ProcessDefinitionRevisionDto save(ProcessDefinitionRevisionDto dto) {
        ProcessDefinitionRevision created = new ProcessDefinitionRevision(dto.getData(), dto.getProcessDefinitionId());
        ProcessDefinitionRevision saved = processDefinitionRevisionRepository.save(created);
        return mapper.entityToDto(saved);
    }

    @Transactional
    @Override
    public void delete(UUID id) {
        processDefinitionRevisionRepository.deleteById(id);
    }
}
