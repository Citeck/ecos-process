package ru.citeck.ecos.process.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class ProcessDefinitionRevisionDto {

    @NotNull
    private byte[] data;

    @NotNull
    private String processDefinitionId;
}
