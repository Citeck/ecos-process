package ru.citeck.ecos.process.domain.procdef.command.finddef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FindProcDefResp {
    private String procDefId;
    private String procDefRevId;
}
