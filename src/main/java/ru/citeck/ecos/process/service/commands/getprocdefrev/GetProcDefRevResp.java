package ru.citeck.ecos.process.service.commands.getprocdefrev;

import lombok.Data;

@Data
public class GetProcDefRevResp {
    private String id;
    private String format;
    private byte[] data;
    private String procDefId;
    private int version;
}
