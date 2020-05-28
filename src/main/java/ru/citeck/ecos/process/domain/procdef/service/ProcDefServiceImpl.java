package ru.citeck.ecos.process.domain.procdef.service;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.apps.module.ModuleRef;
import ru.citeck.ecos.process.domain.common.entity.EntityUuid;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto;
import ru.citeck.ecos.process.domain.procdef.entity.ProcDefEntity;
import ru.citeck.ecos.process.domain.procdef.entity.ProcDefRevEntity;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.repository.ProcDefRepository;
import ru.citeck.ecos.process.domain.procdef.repository.ProcDefRevRepository;
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProcDefServiceImpl implements ProcDefService {

    private final ProcDefRepository procDefRepo;
    private final ProcDefRevRepository processDefRevRepo;

    private final ProcTenantService tenantService;
    private final RecordsService recordsService;

    @Override
    public ProcDefDto uploadProcDef(NewProcessDefDto processDef) {

        int currentTenant = tenantService.getCurrent();

        Instant now = Instant.now();

        ProcDefEntity currentProcDef = procDefRepo.findFirstByIdTntAndProcTypeAndExtId(
            currentTenant,
            processDef.getProcType(),
            processDef.getId()
        ).orElse(null);

        ProcDefRevEntity newRevision = new ProcDefRevEntity();
        newRevision.setId(new EntityUuid(tenantService.getCurrent(), UUID.randomUUID()));
        newRevision.setCreated(now);
        newRevision.setData(processDef.getData());
        newRevision.setFormat(processDef.getFormat());

        if (currentProcDef == null) {

            currentProcDef = new ProcDefEntity();
            currentProcDef.setId(new EntityUuid(tenantService.getCurrent(), UUID.randomUUID()));
            currentProcDef.setAlfType(processDef.getAlfType());
            currentProcDef.setEcosTypeRef(processDef.getEcosTypeRef().toString());
            currentProcDef.setExtId(processDef.getId());
            currentProcDef.setProcType(processDef.getProcType());
            currentProcDef.setModified(now);
            currentProcDef.setCreated(now);
            currentProcDef.setEnabled(true);
            currentProcDef = procDefRepo.save(currentProcDef);

            newRevision.setVersion(0);

        } else {

            currentProcDef.setAlfType(processDef.getAlfType());
            currentProcDef.setEcosTypeRef(processDef.getEcosTypeRef().toString());
            currentProcDef.setModified(now);
            if (currentProcDef.getEnabled() == null) {
                currentProcDef.setEnabled(true);
            }

            newRevision.setVersion(currentProcDef.getLastRev().getVersion() + 1);
            newRevision.setPrevRev(currentProcDef.getLastRev());
        }

        newRevision.setProcessDef(currentProcDef);
        newRevision = processDefRevRepo.save(newRevision);

        currentProcDef.setLastRev(newRevision);
        currentProcDef = procDefRepo.save(currentProcDef);

        return procDefToDto(currentProcDef);
    }

    @Override
    public List<ProcDefWithDataDto> findAll(Predicate predicate, int max, int skip) {

        int currentTenant = tenantService.getCurrent();

        PredicateQuery query;
        if (predicate == null) {
            query = new PredicateQuery();
        } else {
            query = PredicateUtils.convertToDto(predicate, PredicateQuery.class);
        }
        PageRequest page = PageRequest.of(skip / max, max);

        List<ProcDefEntity> entities;
        if (StringUtils.isBlank(query.moduleId)) {
            entities = procDefRepo.findAllByIdTnt(currentTenant, page);
        } else {
            entities = procDefRepo.findAllByIdTntAndExtIdLike(
                currentTenant,
                "%" + query.getModuleId() + "%",
                page
            );
        }

        return entities.stream().map(entity -> {

            ProcDefDto procDefDto = procDefToDto(entity);
            ProcDefRevDto procDefRevDto = procDefRevToDto(entity.getLastRev());
            return new ProcDefWithDataDto(procDefDto, procDefRevDto);

        }).collect(Collectors.toList());
    }

    @Override
    public long getCount() {
        return getCount(null);
    }

    public long getCount(Predicate predicate) {
        int currentTenant = tenantService.getCurrent();

        PredicateQuery query;
        if (predicate == null) {
            query = new PredicateQuery();
        } else {
            query = PredicateUtils.convertToDto(predicate, PredicateQuery.class);
        }

        if (StringUtils.isBlank(query.moduleId)) {
            return procDefRepo.getCount(currentTenant);
        } else {
            return procDefRepo.getCount(
                currentTenant,
                "%" + query.getModuleId() + "%"
            );
        }
    }

    @Override
    public ProcDefDto uploadNewRev(ProcDefWithDataDto dto) {

        int currentTenant = tenantService.getCurrent();

        String id = dto.getId();
        String procType = dto.getProcType();

        ProcDefEntity procDefEntity =
            procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, procType, id)
                .orElseThrow(() -> new IllegalArgumentException("Process definition is not found by id " + id));

        byte[] currentData = procDefEntity.getLastRev().getData();

        ProcDefDto result;
        if (!Arrays.equals(currentData, dto.getData())) {

            NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
            newProcessDefDto.setAlfType(dto.getAlfType());
            newProcessDefDto.setData(dto.getData());
            newProcessDefDto.setEcosTypeRef(dto.getEcosTypeRef());
            newProcessDefDto.setFormat(dto.getFormat());
            newProcessDefDto.setId(id);
            newProcessDefDto.setProcType(procType);

            result = uploadProcDef(newProcessDefDto);

        } else {

            procDefEntity.setAlfType(dto.getAlfType());
            procDefEntity.setEcosTypeRef(String.valueOf(dto.getEcosTypeRef()));
            if (dto.getEnabled() != null) {
                procDefEntity.setEnabled(dto.getEnabled());
            }
            result = procDefToDto(procDefRepo.save(procDefEntity));
        }

        return result;
    }

    @Override
    public Optional<ProcDefRevDto> getProcessDefRev(String procType, UUID procDefRevId) {

        EntityUuid revId = new EntityUuid(tenantService.getCurrent(), procDefRevId);
        ProcDefRevEntity revEntity = processDefRevRepo.findById(revId).orElse(null);

        if (revEntity == null) {
            return Optional.empty();
        }
        return Optional.of(procDefRevToDto(revEntity));
    }

    @Override
    public Optional<ProcDefWithDataDto> getProcessDefById(ModuleRef id) {

        int currentTenant = tenantService.getCurrent();

        Optional<ProcDefEntity> procDefEntity =
            procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, id.getType(), id.getId());

        return procDefEntity.map(def ->
            new ProcDefWithDataDto(procDefToDto(def), procDefRevToDto(def.getLastRev()))
        );
    }

    @Override
    public Optional<ProcDefRevDto> findProcDef(String type, RecordRef ecosTypeRef, List<String> alfTypes) {

        int currentTenant = tenantService.getCurrent();

        ProcDefEntity processDef = null;

        if (RecordRef.isNotEmpty(ecosTypeRef)) {

            String ecosType = ecosTypeRef.toString();
            processDef = procDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(
                currentTenant,
                type,
                ecosType
            ).orElse(null);

            if (processDef == null) {

                TypeParents typeInfo = recordsService.getMeta(ecosTypeRef, TypeParents.class);
                if (typeInfo == null || typeInfo.parents == null) {
                    throw new IllegalArgumentException("ECOS type parents can't be resolved");
                }

                for (RecordRef parentRef : typeInfo.getParents()) {
                    String parentRefStr = parentRef.toString();
                    processDef = procDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(
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
                processDef = procDefRepo.findFirstByIdTntAndProcTypeAndAlfType(
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

    private ProcDefDto procDefToDto(ProcDefEntity entity) {

        ProcDefDto procDefDto = new ProcDefDto();
        procDefDto.setId(entity.getExtId());
        procDefDto.setProcType(entity.getProcType());
        procDefDto.setRevisionId(entity.getLastRev().getId().getId());
        procDefDto.setAlfType(entity.getAlfType());
        procDefDto.setEcosTypeRef(RecordRef.valueOf(entity.getEcosTypeRef()));
        procDefDto.setEnabled(entity.getEnabled());

        return procDefDto;
    }

    private ProcDefRevDto procDefRevToDto(ProcDefRevEntity entity) {

        ProcDefRevDto procDefRevDto = new ProcDefRevDto();
        procDefRevDto.setId(entity.getId().getId());
        procDefRevDto.setCreated(entity.getCreated());
        procDefRevDto.setProcDefId(entity.getProcessDef().getExtId());
        procDefRevDto.setData(entity.getData());
        procDefRevDto.setFormat(entity.getFormat());
        procDefRevDto.setVersion(entity.getVersion());

        return procDefRevDto;
    }

    @Data
    public static class TypeParents {
        private List<RecordRef> parents;
    }

    @Data
    public static class PredicateQuery {
        private String moduleId;
    }
}
