package ru.citeck.ecos.process.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimerCommandEntity {
    private String id;
    private String targetApp;
    private String type;
    private String body;
}
