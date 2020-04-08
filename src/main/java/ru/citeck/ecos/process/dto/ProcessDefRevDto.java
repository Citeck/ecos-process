package ru.citeck.ecos.process.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProcessDefRevDto {

    @NotNull
    private UUID id;

    @NotNull
    private String format;

    @NotNull
    private byte[] data;

    @NotNull
    private String procDefId;

    @NotNull
    private LocalDateTime created;

    @NotNull
    private int version;
}
