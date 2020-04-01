package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.ProcessRevisionDto;

import java.util.Set;
import java.util.UUID;

public interface ProcessRevisionService {

    ProcessRevisionDto getById(UUID id);

    Set<ProcessRevisionDto> getAllByProcessId(UUID processId);

    ProcessRevisionDto save(ProcessRevisionDto dto);

    void delete(UUID id);
}
