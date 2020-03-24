package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.service.dto.ProcessDefinitionDto;

import java.util.Set;

public interface ProcessDefinitionService {

    Set<ProcessDefinitionDto> getAll();

    ProcessDefinitionDto getById(String id);

    ProcessDefinitionDto save(ProcessDefinitionDto dto);

    void delete(String id);
}
