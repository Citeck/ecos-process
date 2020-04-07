package ru.citeck.ecos.process.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
public class ProcessDto {

    private UUID id;

    private Integer tenant;

    private String record;

    private UUID revisionId;

    private UUID definitionRevId;

    private LocalDateTime created;

    private LocalDateTime modified;

    private boolean active;

    public ProcessDto(UUID id, Integer tenant, String record, UUID revisionId, UUID definitionRevId, boolean active) {
        this.id = id;
        this.tenant = tenant;
        this.record = record;
        this.revisionId = revisionId;
        this.definitionRevId = definitionRevId;
        this.active = active;
    }
}
