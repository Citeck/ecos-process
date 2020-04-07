package ru.citeck.ecos.process.service.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.process.domain.ProcessDefinition;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.exception.ResourceNotFoundException;
import ru.citeck.ecos.process.repository.ProcessDefinitionRepository;
import ru.citeck.ecos.process.repository.ProcessDefinitionRevisionRepository;
import ru.citeck.ecos.process.service.ProcessDefinitionService;
import ru.citeck.ecos.process.dto.ProcessDefinitionDto;
import ru.citeck.ecos.process.service.mapper.ProcessDefinitionMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessDefinitionServiceImpl implements ProcessDefinitionService {

    private final ProcessDefinitionRepository processDefinitionRepository;
    private final ProcessDefinitionMapper mapper;
    private final ProcessDefinitionRevisionRepository definitionRevisionRepository;

    @Override
    public Set<ProcessDefinitionDto> getAll() {
        return processDefinitionRepository.findAll().stream()
            .map(mapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public ProcessDefinitionDto getById(@NonNull String id) {
        Optional<ProcessDefinition> optional = processDefinitionRepository.findById(id);
        if (!optional.isPresent()) {
            throw new ResourceNotFoundException("Process definition", "id", id);
        }
        return mapper.entityToDto(optional.get());
    }

    @Transactional
    @Override
    public ProcessDefinitionDto save(@NonNull ProcessDefinitionDto dto) {
        Optional<ProcessDefinition> optional = processDefinitionRepository.findById(dto.getId());
        ProcessDefinition definitionToSave = mapper.dtoToEntity(dto);
        if (!optional.isPresent()) {
            definitionToSave.setId(null);
            definitionToSave.setModified(definitionToSave.getCreated());
        } else {
            definitionToSave = optional.get();
            definitionToSave.setModified(LocalDateTime.now());

            UUID defRevId = dto.getRevisionId();
            if (defRevId != null) {
                ProcessDefinitionRevision pdr = definitionRevisionRepository.findById(defRevId)
                    .orElseThrow(() -> new ResourceNotFoundException("Process definition revision", "id", defRevId));
                definitionToSave.setDefinitionRevision(pdr);
            }

            definitionToSave.setTenant(dto.getTenant());
        }
        ProcessDefinition saved = processDefinitionRepository.save(definitionToSave);
        return mapper.entityToDto(saved);
    }

    @Transactional
    @Override
    public void delete(@NonNull String id) {
        processDefinitionRepository.deleteById(id);
    }
}
