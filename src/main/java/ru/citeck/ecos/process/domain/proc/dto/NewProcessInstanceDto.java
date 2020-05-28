package ru.citeck.ecos.process.domain.proc.dto;

import lombok.Data;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@ToString(exclude = "stateData")
public class NewProcessInstanceDto extends ProcessInstanceDto {

    @NotNull
    private UUID id;

    @NotNull
    private UUID stateId;

    @NotNull
    private byte[] stateData;
}
