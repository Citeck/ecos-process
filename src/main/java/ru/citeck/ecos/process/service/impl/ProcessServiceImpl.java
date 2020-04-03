package ru.citeck.ecos.process.service.impl;

import com.google.common.collect.Sets;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.dto.ProcessDefinitionRevisionDto;
import ru.citeck.ecos.process.dto.ProcessDto;
import ru.citeck.ecos.process.dto.ProcessRevisionDto;
import ru.citeck.ecos.process.exception.ResourceNotFoundException;
import ru.citeck.ecos.process.repository.ProcessDefinitionRevisionRepository;
import ru.citeck.ecos.process.repository.ProcessRepository;
import ru.citeck.ecos.process.repository.ProcessRevisionRepository;
import ru.citeck.ecos.process.service.ProcessService;
import ru.citeck.ecos.process.service.mapper.ProcessMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final ProcessRepository processRepository;
    private final ProcessMapper processMapper;
    private final ProcessRevisionRepository revisionRepository;
    private final ProcessDefinitionRevisionRepository definitionRevisionRepository;

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
    public Set<ProcessDto> getAll(Set<UUID> uuids) {
        Iterable<Process> iterable = processRepository.findAllById(uuids);
        return Sets.newHashSet(iterable).stream()
            .map(processMapper::entityToDto)
            .collect(Collectors.toSet());
    }

    @Override
    @Transactional
    public ProcessDto save(@NonNull ProcessDto dto) {

        Optional<Process> optional = processRepository.findById(dto.getId());

        Process process = new Process();
        if (optional.isPresent()) {
            process = optional.get();
        }

        process.setTenant(dto.getTenant());
        process.setRecord(dto.getRecord());
        process.setActive(dto.isActive());

        UUID defRevId = dto.getDefinitionRevId();
        if (defRevId != null) {
            ProcessDefinitionRevision defRev = definitionRevisionRepository.findById(defRevId)
                .orElseThrow(() -> new ResourceNotFoundException("Process definition revision", "id", defRevId));
            process.setDefinitionRevision(defRev);
        }

        UUID revId = dto.getRevisionId();
        ProcessRevision processRevision;
        if (revId != null) {
            processRevision = revisionRepository.findById(revId)
                .orElseThrow(() -> new ResourceNotFoundException("Process revision", "id", revId));
        } else {
            processRevision = new ProcessRevision();
            processRevision.setProcess(process);
            processRevision.setVersion(1);
            revisionRepository.save(processRevision);
        }
        process.setRevision(processRevision);

        process.setModified(LocalDateTime.now());

        Process saved = processRepository.save(process);

        return processMapper.entityToDto(saved);
    }

    @Override
    @Transactional
    public void delete(@NonNull UUID id) {
        processRepository.deleteById(id);
    }
}
