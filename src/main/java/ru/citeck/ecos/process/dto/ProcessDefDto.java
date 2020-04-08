package ru.citeck.ecos.process.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ProcessDefDto {

    @NotNull
    private String id;

    @NotNull
    private String procType;

    @NotNull
    private UUID revisionId;
}
