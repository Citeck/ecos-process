package ru.citeck.ecos.process.domain.proc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;
import ru.citeck.ecos.process.domain.proc.converters.ProcConvertersKt;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessInstanceDto;
import ru.citeck.ecos.process.domain.proc.dto.ProcessInstanceDto;
import ru.citeck.ecos.process.domain.proc.dto.ProcessStateDto;
import ru.citeck.ecos.process.domain.proc.repo.ProcInstanceRepository;
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository;
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity;
import ru.citeck.ecos.process.domain.proc.repo.ProcessStateEntity;
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity;
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcServiceImpl implements ProcService {

    private final ProcInstanceRepository processRepo;
    private final ProcStateRepository processStateRepo;

    private final ProcDefRevRepository processDefRevRepo;

    @Override
    public NewProcessInstanceDto createProcessInstance(EntityRef recordRef, UUID procDefRevId) {

        EntityUuid procDefId = new EntityUuid(0, procDefRevId);
        ProcDefRevEntity processDefRev = processDefRevRepo.findById(procDefId);

        if (EntityRef.isEmpty(recordRef)) {
            throw new IllegalArgumentException("recordRef can't be empty");
        }
        if (processDefRev == null) {
            throw new IllegalArgumentException("Process definition revision doesn't exists: " + procDefRevId);
        }

        ProcessInstanceEntity processInstance = new ProcessInstanceEntity();
        processInstance.setRecordRef(recordRef.toString());

        Instant now = Instant.now();
        processInstance.setModified(now);
        processInstance.setCreated(now);
        processInstance.setProcType(processDefRev.getProcessDef().getProcType());

        processInstance = processRepo.save(processInstance);

        ProcessStateEntity state = new ProcessStateEntity();
        state.setData(new byte[0]);
        state.setProcDefRev(processDefRev);
        state.setProcess(processInstance);
        state.setVersion(0);
        state = processStateRepo.save(state);

        processInstance.setState(state);
        processInstance = processRepo.save(processInstance);

        NewProcessInstanceDto newProcessDto = new NewProcessInstanceDto(
            processInstance.getId().getId(),
            state.getId().getId(),
            state.getData()
        );

        return newProcessDto;
    }

    @Override
    public ProcessInstanceDto getInstanceById(UUID id) {

        return Optional.ofNullable(processRepo.findById(new EntityUuid(0, id)))
            .map(ProcConvertersKt::toDto)
            .orElse(null);

    }

    @Override
    public ProcessStateDto updateStateData(UUID prevStateId, byte[] data) {

        EntityUuid entityId = new EntityUuid(0, prevStateId);

        ProcessStateEntity stateEntity = Optional.ofNullable(processStateRepo.findById(entityId))
            .orElseThrow(() ->
                new IllegalArgumentException("Process state with id " + prevStateId + " doesn't exists"));

        ProcessStateEntity newState = new ProcessStateEntity();
        newState.setData(data);
        newState.setProcDefRev(stateEntity.getProcDefRev());
        newState.setProcess(stateEntity.getProcess());
        newState.setVersion(stateEntity.getVersion() + 1);
        newState.setCreated(Instant.now());
        newState = processStateRepo.save(newState);

        ProcessInstanceEntity process = stateEntity.getProcess();
        process.setState(newState);
        process.setModified(Instant.now());
        processRepo.save(process);

        return ProcConvertersKt.toDto(newState);
    }

    @Override
    public ProcessStateDto getProcStateByProcId(UUID procId) {

        return Optional.ofNullable(processRepo.findById(new EntityUuid(0, procId)))
            .map(ProcessInstanceEntity::getState)
            .map(ProcConvertersKt::toDto)
            .orElse(null);
    }

    @Override
    public ProcessStateDto getProcStateByStateId(UUID procStateId) {

        return Optional.ofNullable(processStateRepo.findById(new EntityUuid(0, procStateId)))
            .map(ProcConvertersKt::toDto)
            .orElse(null);
    }
}
