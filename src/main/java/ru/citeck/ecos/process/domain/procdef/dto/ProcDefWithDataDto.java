package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ru.citeck.ecos.commons.data.MLText;
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

    private MLText name;

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
        this.name = other.name;
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
        this.name = def.getName();
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public MLText getName() {
        return name;
    }

    public void setName(MLText name) {
        this.name = name;
    }

    public String getProcType() {
        return procType;
    }

    public void setProcType(String procType) {
        this.procType = procType;
    }

    public RecordRef getEcosTypeRef() {
        return ecosTypeRef;
    }

    public void setEcosTypeRef(RecordRef ecosTypeRef) {
        this.ecosTypeRef = ecosTypeRef;
    }

    public String getAlfType() {
        return alfType;
    }

    public void setAlfType(String alfType) {
        this.alfType = alfType;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        this.modified = modified;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
