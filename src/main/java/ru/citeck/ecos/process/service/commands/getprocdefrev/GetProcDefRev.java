package ru.citeck.ecos.process.service.commands.getprocdefrev;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;

@Data
@CommandType("get-proc-def-rev")
@AllArgsConstructor
@NoArgsConstructor
public class GetProcDefRev {
    private String procType;
    private String procDefRevId;
}
