package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
@ToString(exclude = "data")
public class ProcDefWithDataDto {

    @NotNull
    private String id;

    private MLText name;

    @NotNull
    private String procType;

    private String workspace;

    @NotNull
    private EntityRef ecosTypeRef;

    @NotNull
    private String alfType;

    @NotNull
    private UUID revisionId;

    @NotNull
    private String format;

    @NotNull
    private byte[] data;

    @NotNull
    private byte[] image;

    @NotNull
    private Instant modified;

    @NotNull
    private Instant created;

    @NotNull
    private int version;

    private EntityRef formRef;

    private EntityRef workingCopySourceRef;

    private Boolean enabled;

    private Boolean autoStartEnabled;

    private Boolean autoDeleteEnabled;

    private EntityRef sectionRef = EntityRef.EMPTY;

    private EntityRef createdFromVersion = EntityRef.EMPTY;

    public ProcDefWithDataDto() {
    }

    public ProcDefWithDataDto(ProcDefWithDataDto other) {
        this.id = other.id;
        this.name = other.name;
        this.procType = other.procType;
        this.revisionId = other.revisionId;
        this.format = other.format;
        this.data = other.data;
        this.image = other.image;
        this.ecosTypeRef = other.ecosTypeRef;
        this.alfType = other.alfType;
        this.modified = other.modified;
        this.created = other.created;
        this.version = other.version;
        this.formRef = other.formRef;
        this.workingCopySourceRef = other.workingCopySourceRef;
        this.enabled = other.enabled;
        this.autoStartEnabled = other.autoStartEnabled;
        this.autoDeleteEnabled = other.autoDeleteEnabled;
        setSectionRef(other.sectionRef);
    }

    public ProcDefWithDataDto(ProcDefDto def, ProcDefRevDto rev, ProcDefRevDataProvider dataProvider) {
        this.id = def.getId();
        this.name = def.getName();
        this.procType = def.getProcType();
        this.workspace = def.getWorkspace();
        this.revisionId = def.getRevisionId();
        this.format = rev.getFormat();
        this.data = rev.loadData(dataProvider);
        this.image = rev.getImage();
        this.ecosTypeRef = def.getEcosTypeRef();
        this.alfType = def.getAlfType();
        this.modified = rev.getCreated();
        this.created = def.getCreated();
        this.version = rev.getVersion();
        this.formRef = def.getFormRef();
        this.workingCopySourceRef = def.getWorkingCopySourceRef();
        this.enabled = def.getEnabled();
        this.autoStartEnabled = def.getAutoStartEnabled();
        this.autoDeleteEnabled = def.getAutoDeleteEnabled();
        setSectionRef(def.getSectionRef());
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

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace != null ? workspace : "";
    }

    public EntityRef getEcosTypeRef() {
        return ecosTypeRef;
    }

    public void setEcosTypeRef(EntityRef ecosTypeRef) {
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

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
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

    public EntityRef getFormRef() {
        return formRef;
    }

    public void setFormRef(EntityRef formRef) {
        this.formRef = formRef;
    }

    public Boolean getAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(Boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }

    public Boolean getAutoDeleteEnabled() {
        return autoDeleteEnabled;
    }

    public void setAutoDeleteEnabled(Boolean autoDeleteEnabled) {
        this.autoDeleteEnabled = autoDeleteEnabled;
    }

    public EntityRef getSectionRef() {
        return sectionRef;
    }

    public EntityRef getCreatedFromVersion() {
        return createdFromVersion;
    }

    public EntityRef getWorkingCopySourceRef() {
        return workingCopySourceRef;
    }

    public void setWorkingCopySourceRef(EntityRef workingCopySourceRef) {
        this.workingCopySourceRef = workingCopySourceRef;
    }

    public void setCreatedFromVersion(EntityRef createdFromVersion) {
        this.createdFromVersion = createdFromVersion;
    }

    public void setSectionRef(EntityRef sectionRef) {
        if (sectionRef == null) {
            this.sectionRef = EntityRef.EMPTY;
        } else {
            this.sectionRef = sectionRef;
        }
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        if (created != null) {
            this.created = created;
        } else {
            this.created = Instant.EPOCH;
        }
    }
}
