package ru.citeck.ecos.process.service.commands.createproc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProcResp {
    private String procId;
    private String procStateId;
    private byte[] procStateData;
}
