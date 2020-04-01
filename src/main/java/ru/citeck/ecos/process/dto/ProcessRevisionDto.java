package ru.citeck.ecos.process.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProcessRevisionDto {

    private UUID id;

    private byte[] data;

    private UUID processId;

    private LocalDateTime created;

    private Integer version;
}
