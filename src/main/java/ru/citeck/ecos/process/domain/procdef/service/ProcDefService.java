package ru.citeck.ecos.process.domain.procdef.service;

import ru.citeck.ecos.apps.artifact.ArtifactRef;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.Predicate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcDefService {

    ProcDefDto uploadProcDef(NewProcessDefDto processDef);

    ProcDefDto uploadNewRev(ProcDefWithDataDto dto);

    List<ProcDefWithDataDto> findAllWithData(Predicate predicate, int max, int skip);

    List<ProcDefDto> findAll(Predicate predicate, int max, int skip);

    long getCount();

    long getCount(Predicate predicate);

    String getCacheKey();

    Optional<ProcDefRevDto> getProcessDefRev(String procType, UUID procDefRevId);

    Optional<ProcDefRevDto> findProcDef(String procType, RecordRef ecosTypeRef, List<String> altTypes);

    Optional<ProcDefWithDataDto> getProcessDefById(ArtifactRef id);
}
