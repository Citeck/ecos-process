package ru.citeck.ecos.process.service.commands.getprocstate;

import lombok.Data;
import ru.citeck.ecos.commands.annotation.CommandType;

@Data
@CommandType("get-proc-state")
public class GetProcState {
    private String procType;
    private String procStateId;
}
