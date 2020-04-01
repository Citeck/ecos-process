package ru.citeck.ecos.process.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
public class ProcessDefinitionRevisionDto {

    private UUID id;

    @NotNull
    private byte[] data;

    @NotNull
    private String processDefinitionId;

    private LocalDateTime created;

    private Integer version;
}
