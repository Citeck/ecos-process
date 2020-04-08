package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.NewProcessInstanceDto;
import ru.citeck.ecos.process.dto.ProcessInstanceDto;
import ru.citeck.ecos.process.dto.ProcessStateDto;
import ru.citeck.ecos.records2.RecordRef;

import java.util.UUID;

public interface ProcessService {

    NewProcessInstanceDto createProcessInstance(RecordRef recordRef, UUID procDefRevId);

    ProcessInstanceDto getInstanceById(UUID id);

    ProcessStateDto updateStateData(UUID prevStateId, byte[] data);

    ProcessStateDto getProcStateByProcId(String procType, UUID procId);

    ProcessStateDto getProcStateByStateId(String procType, UUID procStateId);
}
