package ru.citeck.ecos.process.service.commands.updateprocstate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;

@Data
@CommandType("update-proc-state")
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProcState {
    private String prevProcStateId;
    private byte[] stateData;
}
