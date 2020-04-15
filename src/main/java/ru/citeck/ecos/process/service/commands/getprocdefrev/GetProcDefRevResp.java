package ru.citeck.ecos.process.service.commands.getprocdefrev;

import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "data")
public class GetProcDefRevResp {
    private String id;
    private String format;
    private byte[] data;
    private String procDefId;
    private int version;
}
