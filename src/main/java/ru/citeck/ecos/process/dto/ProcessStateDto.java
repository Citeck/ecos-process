package ru.citeck.ecos.process.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProcessStateDto {

    @NotNull
    private UUID id;

    @NotNull
    private byte[] data;

    @NotNull
    private UUID processId;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private int version;

    @NotNull
    private UUID procDefRevId;
}
