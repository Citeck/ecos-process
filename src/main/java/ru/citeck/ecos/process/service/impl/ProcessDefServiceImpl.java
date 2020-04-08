package ru.citeck.ecos.process.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.ProcessDefEntity;
import ru.citeck.ecos.process.domain.ProcessDefRevEntity;
import ru.citeck.ecos.process.dto.NewProcessDefDto;
import ru.citeck.ecos.process.dto.ProcessDefRevDto;
import ru.citeck.ecos.process.repository.ProcessDefRepository;
import ru.citeck.ecos.process.repository.ProcessDefRevRepository;
import ru.citeck.ecos.process.service.ProcessDefService;
import ru.citeck.ecos.process.dto.ProcessDefDto;
import ru.citeck.ecos.records2.RecordRef;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessDefServiceImpl implements ProcessDefService {

    private final ProcessDefRepository processDefRepo;
    private final ProcessDefRevRepository processDefRevRepo;

    private final ProcessTenantService tenantService;

    @Override
    public ProcessDefDto uploadProcDef(NewProcessDefDto processDef) {

        LocalDateTime now = LocalDateTime.now();

        ProcessDefEntity currentProcDef = processDefRepo.findFirstByProcTypeAndExtId(
            processDef.getType(),
            processDef.getId()
        ).orElse(null);

        ProcessDefRevEntity newRevision = new ProcessDefRevEntity();
        newRevision.setId(new EntityUuid(tenantService.getCurrent(), UUID.randomUUID()));
        newRevision.setCreated(now);
        newRevision.setData(processDef.getData());
        newRevision.setFormat(processDef.getFormat());

        if (currentProcDef == null) {

            currentProcDef = new ProcessDefEntity();
            currentProcDef.setId(new EntityUuid(tenantService.getCurrent(), UUID.randomUUID()));
            currentProcDef.setEcosTypeRef(processDef.getEcosTypeRef().toString());
            currentProcDef.setExtId(processDef.getId());
            currentProcDef.setProcType(processDef.getType());
            currentProcDef.setModified(now);
            currentProcDef.setCreated(now);
            currentProcDef = processDefRepo.save(currentProcDef);

            newRevision.setVersion(0);

        } else {

            currentProcDef.setEcosTypeRef(processDef.getEcosTypeRef().toString());
            currentProcDef.setModified(now);

            newRevision.setVersion(currentProcDef.getLastRev().getVersion() + 1);
            newRevision.setPrevRev(currentProcDef.getLastRev());
        }

        newRevision.setProcessDef(currentProcDef);
        newRevision = processDefRevRepo.save(newRevision);

        currentProcDef.setLastRev(newRevision);
        currentProcDef = processDefRepo.save(currentProcDef);

        return new ProcessDefDto(
            currentProcDef.getExtId(),
            currentProcDef.getProcType(),
            newRevision.getId().getId()
        );
    }

    @Override
    public Optional<ProcessDefRevDto> getProcessDefRev(String procType, UUID procDefRevId) {

        EntityUuid revId = new EntityUuid(tenantService.getCurrent(), procDefRevId);
        ProcessDefRevEntity revEntity = processDefRevRepo.findById(revId).orElse(null);

        if (revEntity == null) {
            return Optional.empty();
        }
        return Optional.of(procDefRevToDto(revEntity));
    }

    @Override
    public Optional<ProcessDefRevDto> findProcDef(String type, RecordRef ecosTypeRef) {

        String ecosType = ecosTypeRef.toString();
        ProcessDefEntity processDef = processDefRepo.findFirstByProcTypeAndEcosTypeRef(type, ecosType).orElse(null);

        if (processDef == null) {
            return Optional.empty();
        }
        return Optional.of(procDefRevToDto(processDef.getLastRev()));
    }


    private ProcessDefRevDto procDefRevToDto(ProcessDefRevEntity entity) {

        ProcessDefRevDto processDefRevDto = new ProcessDefRevDto();
        processDefRevDto.setId(entity.getId().getId());
        processDefRevDto.setCreated(entity.getCreated());
        processDefRevDto.setProcDefId(entity.getProcessDef().getExtId());
        processDefRevDto.setData(entity.getData());
        processDefRevDto.setFormat(entity.getFormat());
        processDefRevDto.setVersion(entity.getVersion());

        return processDefRevDto;
    }
}
