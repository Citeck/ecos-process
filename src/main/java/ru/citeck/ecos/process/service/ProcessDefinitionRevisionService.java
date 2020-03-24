package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.service.dto.ProcessDefinitionRevisionDto;

import java.util.Set;
import java.util.UUID;

public interface ProcessDefinitionRevisionService {

    Set<ProcessDefinitionRevisionDto> getAll();

    ProcessDefinitionRevisionDto getById(UUID id);

    Set<ProcessDefinitionRevisionDto> getAllByProcessDefinitionId(String processDefinitionId);

    ProcessDefinitionRevisionDto save(ProcessDefinitionRevisionDto dto);

    void delete(UUID id);
}
