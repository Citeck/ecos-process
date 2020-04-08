package ru.citeck.ecos.process.service.commands.createproc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;
import ru.citeck.ecos.records2.RecordRef;

@Data
@CommandType("create-proc-instance")
@AllArgsConstructor
@NoArgsConstructor
public class CreateProc {
    private String procDefRevId;
    private RecordRef recordRef;
}
