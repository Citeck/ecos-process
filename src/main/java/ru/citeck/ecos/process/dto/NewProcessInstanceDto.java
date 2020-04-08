package ru.citeck.ecos.process.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
public class NewProcessInstanceDto extends ProcessInstanceDto {

    @NotNull
    private UUID id;

    @NotNull
    private UUID stateId;

    @NotNull
    private byte[] stateData;
}
