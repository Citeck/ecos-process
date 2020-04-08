package ru.citeck.ecos.process.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.records2.RecordRef;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
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
    private LocalDateTime created;

    @NotNull
    private LocalDateTime modified;
}
