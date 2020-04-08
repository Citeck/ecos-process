package ru.citeck.ecos.process.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessInstanceEntity;
import ru.citeck.ecos.process.domain.ProcessDefRevEntity;
import ru.citeck.ecos.process.domain.ProcessStateEntity;
import ru.citeck.ecos.process.dto.NewProcessInstanceDto;
import ru.citeck.ecos.process.dto.ProcessInstanceDto;
import ru.citeck.ecos.process.dto.ProcessStateDto;
import ru.citeck.ecos.process.repository.ProcessDefRevRepository;
import ru.citeck.ecos.process.repository.ProcessInstanceRepository;
import ru.citeck.ecos.process.repository.ProcessStateRepository;
import ru.citeck.ecos.process.service.ProcessService;
import ru.citeck.ecos.records2.RecordRef;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessServiceImpl implements ProcessService {

    private final ProcessInstanceRepository processRepo;
    private final ProcessDefRevRepository processDefRevRepo;
    private final ProcessStateRepository processStateRepo;

    private final ProcessTenantService tenantService;

    @Override
    public NewProcessInstanceDto createProcessInstance(RecordRef recordRef, UUID procDefRevId) {

        EntityUuid procDefId = new EntityUuid(tenantService.getCurrent(), procDefRevId);
        ProcessDefRevEntity processDefRev = processDefRevRepo.findById(procDefId).orElse(null);

        if (RecordRef.isEmpty(recordRef)) {
            throw new IllegalArgumentException("recordRef can't be empty");
        }
        if (processDefRev == null) {
            throw new IllegalArgumentException("Process definition revision doesn't exists: " + procDefRevId);
        }

        ProcessInstanceEntity processInstance = new ProcessInstanceEntity();
        processInstance.setId(new EntityUuid(tenantService.getCurrent(), UUID.randomUUID()));
        processInstance.setRecordRef(recordRef.toString());

        LocalDateTime now = LocalDateTime.now();
        processInstance.setModified(now);
        processInstance.setCreated(now);
        processInstance.setProcType(processDefRev.getProcessDef().getProcType());

        processInstance = processRepo.save(processInstance);

        ProcessStateEntity state = new ProcessStateEntity();
        state.setId(new EntityUuid(tenantService.getCurrent(), UUID.randomUUID()));
        state.setData(new byte[0]);
        state.setProcDefRev(processDefRev);
        state.setProcess(processInstance);
        state.setVersion(0);
        state = processStateRepo.save(state);

        processInstance.setState(state);
        processInstance = processRepo.save(processInstance);

        NewProcessInstanceDto newProcessDto = new NewProcessInstanceDto();
        newProcessDto.setId(processInstance.getId().getId());
        newProcessDto.setStateId(state.getId().getId());
        newProcessDto.setStateData(state.getData());

        return newProcessDto;
    }

    @Override
    public ProcessInstanceDto getInstanceById(UUID id) {
        return processToDto(processRepo.findById(new EntityUuid(tenantService.getCurrent(), id)).orElse(null));
    }

    @Override
    public ProcessStateDto updateStateData(UUID prevStateId, byte[] data) {

        int currentTenant = tenantService.getCurrent();
        EntityUuid entityId = new EntityUuid(currentTenant, prevStateId);

        ProcessStateEntity stateEntity = processStateRepo.findById(entityId)
            .orElseThrow(() ->
                new IllegalArgumentException("Process state with id " + prevStateId + " doesn't exists"));

        ProcessStateEntity newState = new ProcessStateEntity();
        newState.setId(new EntityUuid(currentTenant, UUID.randomUUID()));
        newState.setData(data);
        newState.setProcDefRev(stateEntity.getProcDefRev());
        newState.setProcess(stateEntity.getProcess());
        newState.setVersion(stateEntity.getVersion() + 1);
        newState.setCreated(LocalDateTime.now());
        newState = processStateRepo.save(newState);

        ProcessInstanceEntity process = stateEntity.getProcess();
        process.setState(newState);
        process.setModified(LocalDateTime.now());
        processRepo.save(process);

        return stateToDto(newState);
    }

    @Override
    public ProcessStateDto getProcStateByProcId(String procType, UUID procId) {

        return processRepo.findById(new EntityUuid(tenantService.getCurrent(), procId))
            .map(ProcessInstanceEntity::getState)
            .map(this::stateToDto)
            .orElse(null);
    }

    @Override
    public ProcessStateDto getProcStateByStateId(String procType, UUID procStateId) {

        return processStateRepo.findById(new EntityUuid(tenantService.getCurrent(), procStateId))
            .map(this::stateToDto)
            .orElse(null);
    }

    private ProcessStateDto stateToDto(ProcessStateEntity entity) {

        ProcessStateDto processStateDto = new ProcessStateDto();
        processStateDto.setId(entity.getId().getId());
        processStateDto.setCreated(entity.getCreated());
        processStateDto.setProcessId(entity.getProcess().getId().getId());
        processStateDto.setData(entity.getData());
        processStateDto.setVersion(entity.getVersion());
        processStateDto.setProcDefRevId(entity.getProcDefRev().getId().getId());

        return processStateDto;
    }

    private ProcessInstanceDto processToDto(ProcessInstanceEntity entity) {

        if (entity == null) {
            return null;
        }

        ProcessInstanceDto processDto = new ProcessInstanceDto();
        processDto.setId(entity.getId().getId());
        processDto.setCreated(entity.getCreated());
        processDto.setModified(entity.getModified());
        processDto.setRecordRef(RecordRef.valueOf(entity.getRecordRef()));
        processDto.setProcType(entity.getProcType());
        processDto.setStateId(entity.getState().getId().getId());

        return processDto;
    }
}
