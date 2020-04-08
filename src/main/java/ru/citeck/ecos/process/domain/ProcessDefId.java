package ru.citeck.ecos.process.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcessDefId {
    private int tenant;
    private String type;
    private String id;
}
