package ru.citeck.ecos.process.domain.proc.dto;

import lombok.Data;
import ru.citeck.ecos.records2.RecordRef;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
public class ProcessInstanceDto {

    @NotNull
    private UUID id;

    @NotNull
    private String procType;

    @NotNull
    private RecordRef recordRef;

    @NotNull
    private UUID stateId;

    @NotNull
    private Instant created;

    @NotNull
    private Instant modified;
}
