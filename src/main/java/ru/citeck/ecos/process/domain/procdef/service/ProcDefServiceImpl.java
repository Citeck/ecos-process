package ru.citeck.ecos.process.domain.procdef.service;

import com.querydsl.core.types.dsl.BooleanExpression;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto;
import ru.citeck.ecos.process.domain.procdef.repo.*;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.predicate.PredicateUtils;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import javax.annotation.PostConstruct;
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
            currentProcDef.setName(Json.getMapper().toString(processDef.getName()));

            currentProcDef.setModified(now);
            currentProcDef.setCreated(now);
            currentProcDef.setEnabled(true);
            currentProcDef = procDefRepo.save(currentProcDef);

            newRevision.setVersion(0);

        } else {

            currentProcDef.setAlfType(processDef.getAlfType());
            currentProcDef.setEcosTypeRef(processDef.getEcosTypeRef().toString());
            currentProcDef.setName(Json.getMapper().toString(processDef.getName()));
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
    public List<ProcDefDto> findAll(Predicate predicate, int max, int skip) {

        return findAllProcDefEntities(predicate, max, skip).stream()
            .map(this::procDefToDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<ProcDefWithDataDto> findAllWithData(Predicate predicate, int max, int skip) {

        return findAllProcDefEntities(predicate, max, skip).stream().map(entity -> {

            ProcDefDto procDefDto = procDefToDto(entity);
            ProcDefRevDto procDefRevDto = procDefRevToDto(entity.getLastRev());
            return new ProcDefWithDataDto(procDefDto, procDefRevDto);

        }).collect(Collectors.toList());
    }

    private List<ProcDefEntity> findAllProcDefEntities(Predicate predicate, int max, int skip) {

        PageRequest page = PageRequest.of(skip / max, max);
        BooleanExpression query = predicateToQuery(predicate);
        return procDefRepo.findAll(query, page).getContent();
    }

    private BooleanExpression predicateToQuery(Predicate predicate) {

        PredicateQuery predQuery = PredicateUtils.convertToDto(predicate, PredicateQuery.class);

        QProcDefEntity entity = QProcDefEntity.procDefEntity;
        BooleanExpression query = entity.id.tnt.eq(tenantService.getCurrent());
        if (StringUtils.isNotBlank(predQuery.procType)) {
            query = query.and(QProcDefEntity.procDefEntity.procType.eq(predQuery.procType));
        }
        if (StringUtils.isNotBlank(predQuery.moduleId)) {
            query = query.and(QProcDefEntity.procDefEntity.extId.likeIgnoreCase("%" + predQuery.moduleId + "%"));
        }

        return query;
    }

    @Override
    public long getCount() {
        return getCount(null);
    }

    public long getCount(Predicate predicate) {
        return procDefRepo.count(predicateToQuery(predicate));
    }

    @Override
    public ProcDefDto uploadNewRev(ProcDefWithDataDto dto) {

        int currentTenant = tenantService.getCurrent();

        String id = dto.getId();
        String procType = dto.getProcType();

        ProcDefEntity procDefEntity =
            procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, procType, id)
                .orElse(null);

        if (procDefEntity == null) {

            NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
            newProcessDefDto.setId(dto.getId());
            newProcessDefDto.setName(dto.getName());
            newProcessDefDto.setData(dto.getData());
            newProcessDefDto.setAlfType(dto.getAlfType());
            newProcessDefDto.setEcosTypeRef(dto.getEcosTypeRef());
            newProcessDefDto.setFormat(dto.getFormat());
            newProcessDefDto.setProcType(dto.getProcType());

            return uploadProcDef(newProcessDefDto);
        }

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
            procDefEntity.setName(Json.getMapper().toString(dto.getName()));

            if (dto.getEnabled() != null) {
                procDefEntity.setEnabled(dto.getEnabled());
            }
            procDefEntity.setModified(Instant.now());

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
    public Optional<ProcDefWithDataDto> getProcessDefById(ProcDefRef id) {

        int currentTenant = tenantService.getCurrent();

        Optional<ProcDefEntity> procDefEntity =
            procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, id.getType(), id.getId());

        return procDefEntity.map(def ->
            new ProcDefWithDataDto(procDefToDto(def), procDefRevToDto(def.getLastRev()))
        );
    }

    @Override
    public String getCacheKey() {

        int currentTenant = tenantService.getCurrent();
        PageRequest page = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("modified")));

        List<ProcDefEntity> modified = procDefRepo.getModifiedDate(currentTenant, page);
        if (modified.isEmpty()) {
            return "";
        }
        return modified.get(0).getModified().toString();
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
                if (typeInfo.parents == null) {
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

    public void delete() {

    }

    private ProcDefDto procDefToDto(ProcDefEntity entity) {

        ProcDefDto procDefDto = new ProcDefDto();
        procDefDto.setId(entity.getExtId());
        procDefDto.setProcType(entity.getProcType());
        procDefDto.setName(Json.getMapper().read(entity.getName(), MLText.class));
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
        private String procType;
    }
}
