package ru.citeck.ecos.process.service.commands.finddef;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;
import ru.citeck.ecos.records2.RecordRef;

@Data
@CommandType("find-proc-def")
@AllArgsConstructor
@NoArgsConstructor
public class FindProcDef {
    private String procType;
    private RecordRef ecosTypeRef;
}
