package ru.citeck.ecos.process.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProcessDto {

    private UUID id;

    private Integer tenant;

    private String record;

    private UUID revisionId;

    private UUID definitionRevId;

    private LocalDateTime created = LocalDateTime.now();

    private LocalDateTime modified;

    private boolean active;
}
