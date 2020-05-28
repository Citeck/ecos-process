package ru.citeck.ecos.process.domain.procdef.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcDefId {
    private int tenant;
    private String type;
    private String id;
}
