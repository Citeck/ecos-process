package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.citeck.ecos.records2.RecordRef;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@ToString(exclude = "data")
public class ProcDefWithDataDto {

    @NotNull
    private String id;

    @NotNull
    private String procType;

    @NotNull
    private RecordRef ecosTypeRef;

    @NotNull
    private String alfType;

    @NotNull
    private UUID revisionId;

    @NotNull
    private String format;

    @NotNull
    private byte[] data;

    @NotNull
    private Instant modified;

    @NotNull
    private int version;

    private Boolean enabled;

    public ProcDefWithDataDto(ProcDefWithDataDto other) {
        this.id = other.id;
        this.procType = other.procType;
        this.revisionId = other.revisionId;
        this.format = other.format;
        this.data = other.data;
        this.ecosTypeRef = other.ecosTypeRef;
        this.alfType = other.alfType;
        this.modified = other.modified;
        this.version = other.version;
        this.enabled = other.enabled;
    }

    public ProcDefWithDataDto(ProcDefDto def, ProcDefRevDto rev) {
        this.id = def.getId();
        this.procType = def.getProcType();
        this.revisionId = def.getRevisionId();
        this.format = rev.getFormat();
        this.data = rev.getData();
        this.ecosTypeRef = def.getEcosTypeRef();
        this.alfType = def.getAlfType();
        this.modified = rev.getCreated();
        this.version = rev.getVersion();
        this.enabled = def.getEnabled();
    }
}
