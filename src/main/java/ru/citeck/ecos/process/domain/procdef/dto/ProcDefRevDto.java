package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
@ToString(exclude = "data")
public class ProcDefRevDto {

    @NotNull
    private UUID id;

    @NotNull
    private String format;

    @NotNull
    private byte[] data;

    @NotNull
    private String procDefId;

    @NotNull
    private Instant created;

    @NotNull
    private int version;
}