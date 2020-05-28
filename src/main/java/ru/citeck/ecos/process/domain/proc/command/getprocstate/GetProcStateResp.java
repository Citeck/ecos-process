package ru.citeck.ecos.process.domain.proc.command.getprocstate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "stateData")
public class GetProcStateResp {
    private String procDefRevId;
    private byte[] stateData;
    private int version;
}
