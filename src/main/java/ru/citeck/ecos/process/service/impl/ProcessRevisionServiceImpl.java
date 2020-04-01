package ru.citeck.ecos.process.service.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.dto.ProcessRevisionDto;
import ru.citeck.ecos.process.exception.ResourceNotFoundException;
import ru.citeck.ecos.process.repository.ProcessRevisionRepository;
import ru.citeck.ecos.process.service.ProcessRevisionService;
import ru.citeck.ecos.process.service.mapper.ProcessRevisionMapper;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessRevisionServiceImpl implements ProcessRevisionService {

    private final ProcessRevisionRepository processRevisionRepository;
    private final ProcessRevisionMapper processRevisionMapper;

    @Override
    public ProcessRevisionDto getById(@NonNull UUID id) {
        ProcessRevision revision = processRevisionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Process revision", "id", id));
        return processRevisionMapper.entityToDto(revision);
    }

    @Override
    public Set<ProcessRevisionDto> getAllByProcessId(@NonNull UUID id) {
        return processRevisionRepository.findProcessRevisionsByProcessId(id).stream()
            .map(processRevisionMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    public ProcessRevisionDto save(@NonNull ProcessRevisionDto dto) {
        ProcessRevision revision = new ProcessRevision(dto.getData(), dto.getProcessId());
        ProcessRevision saved = processRevisionRepository.save(revision);
        return processRevisionMapper.entityToDto(saved);
    }

    @Override
    public void delete(@NonNull UUID id) {
        processRevisionRepository.deleteById(id);
    }
}
