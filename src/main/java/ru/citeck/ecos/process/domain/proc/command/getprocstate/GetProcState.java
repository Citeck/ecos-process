package ru.citeck.ecos.process.domain.proc.command.getprocstate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;

@Data
@CommandType("get-proc-state")
@AllArgsConstructor
@NoArgsConstructor
public class GetProcState {
    private String procType;
    private String procStateId;
}
