package ru.citeck.ecos.process.domain.proc.command.updateprocstate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProcStateResp {
    private String procStateId;
    private int version;
}
