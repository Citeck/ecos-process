package ru.citeck.ecos.process.service.impl;

import lombok.Data;
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
import ru.citeck.ecos.records2.RecordsService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcessDefServiceImpl implements ProcessDefService {

    private final ProcessDefRepository processDefRepo;
    private final ProcessDefRevRepository processDefRevRepo;

    private final ProcessTenantService tenantService;
    private final RecordsService recordsService;

    @Override
    public ProcessDefDto uploadProcDef(NewProcessDefDto processDef) {

        int currentTenant = tenantService.getCurrent();

        Instant now = Instant.now();

        ProcessDefEntity currentProcDef = processDefRepo.findFirstByIdTntAndProcTypeAndExtId(
            currentTenant,
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
            currentProcDef.setAlfType(processDef.getAlfType());
            currentProcDef.setEcosTypeRef(processDef.getEcosTypeRef().toString());
            currentProcDef.setExtId(processDef.getId());
            currentProcDef.setProcType(processDef.getType());
            currentProcDef.setModified(now);
            currentProcDef.setCreated(now);
            currentProcDef = processDefRepo.save(currentProcDef);

            newRevision.setVersion(0);

        } else {

            currentProcDef.setAlfType(processDef.getAlfType());
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
    public Optional<ProcessDefRevDto> findProcDef(String type, RecordRef ecosTypeRef, List<String> alfTypes) {

        int currentTenant = tenantService.getCurrent();

        ProcessDefEntity processDef = null;

        if (RecordRef.isNotEmpty(ecosTypeRef)) {

            String ecosType = ecosTypeRef.toString();
            processDef = processDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRef(
                currentTenant,
                type,
                ecosType
            ).orElse(null);

            if (processDef == null) {
                ecosType = ecosTypeRef.getId();
                processDef = processDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRef(
                    currentTenant,
                    type,
                    ecosType
                ).orElse(null);
            }

            if (processDef == null) {
                int slashIdx = ecosType.indexOf('/');
                if (slashIdx > 0 && ecosType.length() > slashIdx + 1) {
                    ecosType = ecosType.substring(slashIdx + 1);
                    processDef = processDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRef(
                        currentTenant,
                        type,
                        ecosType
                    ).orElse(null);
                }
            }

            if (processDef == null) {

                TypeParents typeInfo = recordsService.getMeta(ecosTypeRef, TypeParents.class);
                if (typeInfo == null || typeInfo.parents == null) {
                    throw new IllegalArgumentException("ECOS type parents can't be resolved");
                }

                for (RecordRef parentRef : typeInfo.getParents()) {
                    String parentRefStr = parentRef.toString();
                    processDef = processDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRef(
                        currentTenant,
                        type,
                        parentRefStr
                    ).orElse(null);
                    if (processDef != null) {
                        break;
                    }
                }
            }
        }

        if (processDef == null && alfTypes != null) {
            for (String alfType : alfTypes) {
                processDef = processDefRepo.findFirstByIdTntAndProcTypeAndAlfType(
                    currentTenant,
                    type,
                    alfType
                ).orElse(null);
                if (processDef != null) {
                    break;
                }
            }
        }

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

    @Data
    public static class TypeParents {
        private List<RecordRef> parents;
    }
}
