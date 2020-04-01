package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.ProcessDto;

import java.util.Set;
import java.util.UUID;

public interface ProcessService {

    ProcessDto getById(UUID id);

    Set<ProcessDto> getAll();

    ProcessDto update(ProcessDto dto);

    ProcessDto save(ProcessDto dto);

    void delete(UUID id);

}
