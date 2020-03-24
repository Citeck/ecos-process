package ru.citeck.ecos.process.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

@Data
@AllArgsConstructor
public class ProcessDefinitionDto {

    private String id;
    private Integer tenant;
    private UUID revisionId;
}
