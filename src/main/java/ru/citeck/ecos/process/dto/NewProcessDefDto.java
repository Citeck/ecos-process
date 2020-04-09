package ru.citeck.ecos.process.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.records2.RecordRef;

@Data
@ToString(exclude = { "data" })
public class NewProcessDefDto {

    private String id;
    private String type;
    private String format;
    private String alfType;
    private RecordRef ecosTypeRef;
    private byte[] data;
}
