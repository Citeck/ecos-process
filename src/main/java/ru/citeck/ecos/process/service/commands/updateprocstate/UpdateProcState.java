package ru.citeck.ecos.process.service.commands.updateprocstate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.citeck.ecos.commands.annotation.CommandType;

@Data
@CommandType("update-proc-state")
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "stateData")
public class UpdateProcState {
    private String prevProcStateId;
    private byte[] stateData;
}
