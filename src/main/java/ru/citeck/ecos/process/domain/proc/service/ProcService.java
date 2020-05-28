package ru.citeck.ecos.process.domain.proc.service;

import ru.citeck.ecos.process.domain.proc.dto.NewProcessInstanceDto;
import ru.citeck.ecos.process.domain.proc.dto.ProcessInstanceDto;
import ru.citeck.ecos.process.domain.proc.dto.ProcessStateDto;
import ru.citeck.ecos.records2.RecordRef;

import java.util.UUID;

public interface ProcService {

    NewProcessInstanceDto createProcessInstance(RecordRef recordRef, UUID procDefRevId);

    ProcessInstanceDto getInstanceById(UUID id);

    ProcessStateDto updateStateData(UUID prevStateId, byte[] data);

    ProcessStateDto getProcStateByProcId(String procType, UUID procId);

    ProcessStateDto getProcStateByStateId(String procType, UUID procStateId);
}
