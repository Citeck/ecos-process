package ru.citeck.ecos.process.service.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.dto.ProcessDto;
import ru.citeck.ecos.process.exception.ResourceNotFoundException;
import ru.citeck.ecos.process.repository.ProcessRepository;
import ru.citeck.ecos.process.service.ProcessService;
import ru.citeck.ecos.process.service.mapper.ProcessMapper;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final ProcessRepository processRepository;
    private final ProcessMapper processMapper;

    @Override
    public ProcessDto getById(@NonNull UUID id) {
        Process process = processRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Process", "id", id));
        return processMapper.entityToDto(process);
    }

    @Override
    public Set<ProcessDto> getAll() {
        return processRepository.findAll().stream()
            .map(processMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public ProcessDto update(@NonNull ProcessDto dto) {
        Process process = processRepository.findById(dto.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Process", "id", dto.getId()));
        process.setActive(dto.isActive());
        process.setDefinitionRevId(dto.getDefinitionRevId());
        process.setRecord(dto.getRecord());
        process.setRevisionId(dto.getRevisionId());
        process.setTenant(dto.getTenant());
        process.setModified(LocalDateTime.now());
        Process saved = processRepository.save(process);
        return processMapper.entityToDto(saved);
    }

    @Override
    public ProcessDto save(@NonNull ProcessDto dto) {
        Process process = new Process();
        process.setTenant(dto.getTenant());
        process.setRecord(dto.getRecord());
        process.setActive(dto.isActive());
        process.setDefinitionRevId(dto.getDefinitionRevId());
        process.setRevisionId(dto.getRevisionId());
        Process saved = processRepository.save(process);
        return processMapper.entityToDto(saved);
    }

    @Override
    public void delete(@NonNull UUID id) {
        processRepository.deleteById(id);
    }
}
