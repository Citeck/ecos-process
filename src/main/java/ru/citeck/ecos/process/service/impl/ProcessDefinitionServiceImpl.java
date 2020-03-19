package ru.citeck.ecos.process.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.process.domain.ProcessDefinition;
import ru.citeck.ecos.process.repository.ProcessDefinitionRepository;
import ru.citeck.ecos.process.service.ProcessDefinitionService;
import ru.citeck.ecos.process.service.dto.ProcessDefinitionDto;
import ru.citeck.ecos.process.service.mapper.ProcessDefinitionMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ProcessDefinitionMapper mapper;

    @Override
    public Set<ProcessDefinitionDto> getAll() {
        return processDefinitionRepository.findAll().stream()
            .map(mapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public ProcessDefinitionDto getById(String id) {
        Optional<ProcessDefinition> optional = processDefinitionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new RuntimeException("Process definition not found by id: " + id);
        }
        return mapper.entityToDto(optional.get());
    }

    @Transactional
    @Override
    public ProcessDefinitionDto save(ProcessDefinitionDto dto) {
        Optional<ProcessDefinition> optional = processDefinitionRepository.findById(dto.getId());
        ProcessDefinition definitionToSave = mapper.dtoToEntity(dto);
        if (!optional.isPresent()) {
            definitionToSave.setId(null);
            definitionToSave.setModified(definitionToSave.getCreated());
        } else {
            definitionToSave = optional.get();
            definitionToSave.setModified(LocalDateTime.now());
            definitionToSave.setRevisionId(dto.getRevisionId());
            definitionToSave.setTenant(dto.getTenant());
        }
        ProcessDefinition saved = processDefinitionRepository.save(definitionToSave);
        return mapper.entityToDto(saved);
    }

    @Transactional
    @Override
    public void delete(String id) {
        processDefinitionRepository.deleteById(id);
    }
}
