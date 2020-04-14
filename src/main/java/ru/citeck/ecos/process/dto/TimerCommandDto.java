package ru.citeck.ecos.process.dto;

import ecos.com.fasterxml.jackson210.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimerCommandDto {
    private String id;
    private String targetApp;
    private String type;
    private JsonNode body;
}
