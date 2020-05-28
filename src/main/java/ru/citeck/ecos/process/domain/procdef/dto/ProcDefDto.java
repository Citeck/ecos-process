package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.records2.RecordRef;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcDefDto {

    @NotNull
    private String id;

    @NotNull
    private String procType;

    @NotNull
    private UUID revisionId;

    @NotNull
    private RecordRef ecosTypeRef;

    @NotNull
    private String alfType;

    private Boolean enabled;
}
