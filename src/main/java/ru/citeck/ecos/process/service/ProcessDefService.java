package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.NewProcessDefDto;
import ru.citeck.ecos.process.dto.ProcessDefDto;
import ru.citeck.ecos.process.dto.ProcessDefRevDto;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessDefService {

    ProcessDefDto uploadProcDef(NewProcessDefDto processDef);

    Optional<ProcessDefRevDto> getProcessDefRev(String procType, UUID procDefRevId);

    Optional<ProcessDefRevDto> findProcDef(String procType, RecordRef ecosTypeRef, List<String> altTypes);
}
