package ru.citeck.ecos.process.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commons.data.ObjectData;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimerCommandDto {
    private String id;
    private String targetApp;
    private String type;
    private ObjectData body;
}
