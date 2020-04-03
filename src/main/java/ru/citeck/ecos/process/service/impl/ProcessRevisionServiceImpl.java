package ru.citeck.ecos.process.service.impl;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.dto.ProcessRevisionDto;
import ru.citeck.ecos.process.exception.BadRequestException;
import ru.citeck.ecos.process.exception.ResourceNotFoundException;
import ru.citeck.ecos.process.repository.ProcessRepository;
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
    private final ProcessRepository processRepository;

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

    @Transactional
    @Override
    public ProcessRevisionDto save(@NonNull ProcessRevisionDto dto) {
        ProcessRevision revision = new ProcessRevision();

        revision.setData(dto.getData());

        UUID processId = dto.getProcessId();
        if (!StringUtils.isEmpty(processId)) {
            Process p = processRepository.findById(processId)
                .orElseThrow(() -> new ResourceNotFoundException("Process", "id", processId));
            revision.setProcess(p);
        } else {
            throw new BadRequestException("Need to provide 'processId' value!");
        }

        ProcessRevision saved = processRevisionRepository.save(revision);
        return processRevisionMapper.entityToDto(saved);
    }

    @Transactional
    @Override
    public void delete(@NonNull UUID id) {
        processRevisionRepository.deleteById(id);
    }
}
