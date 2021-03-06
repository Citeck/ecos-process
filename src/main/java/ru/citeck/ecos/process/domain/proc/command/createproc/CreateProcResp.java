package ru.citeck.ecos.process.domain.proc.command.createproc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "procStateData")
public class CreateProcResp {
    private String procId;
    private String procStateId;
    private byte[] procStateData;
}
