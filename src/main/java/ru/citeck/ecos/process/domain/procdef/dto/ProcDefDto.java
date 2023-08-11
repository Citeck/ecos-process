package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.webapp.api.entity.EntityRef;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

@Data
public class ProcDefDto {

    @NotNull
    private String id;

    private MLText name;

    @NotNull
    private String procType;

    @NotNull
    private String format;

    @NotNull
    private UUID revisionId;

    @NotNull
    private Integer version;

    @NotNull
    private ProcDefRevDataState dataState;

    @NotNull
    private EntityRef ecosTypeRef;

    @NotNull
    private String alfType;

    private EntityRef formRef;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Boolean autoStartEnabled;

    @NotNull
    private EntityRef sectionRef = EntityRef.EMPTY;

    @NotNull
    private Instant created;

    @NotNull
    private Instant modified;

    public ProcDefDto(@NotNull String id,
                      MLText name,
                      @NotNull String procType,
                      @NotNull String format,
                      @NotNull UUID revisionId,
                      @NotNull Integer version,
                      @NotNull EntityRef ecosTypeRef,
                      @NotNull String alfType,
                      EntityRef formRef,
                      Boolean enabled,
                      Boolean autoStartEnabled,
                      @NotNull
                      EntityRef sectionRef,
                      Instant created,
                      Instant modified) {
        this.id = id;
        this.name = name;
        this.procType = procType;
        this.format = format;
        this.revisionId = revisionId;
        this.version = version;
        this.ecosTypeRef = ecosTypeRef;
        this.alfType = alfType;
        this.formRef = formRef;
        this.enabled = enabled;
        this.autoStartEnabled = autoStartEnabled;
        this.sectionRef = sectionRef;
        this.created = created;
        this.modified = modified;
    }

    public ProcDefDto() {}

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

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public UUID getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(UUID revisionId) {
        this.revisionId = revisionId;
    }

    public EntityRef getEcosTypeRef() {
        return ecosTypeRef;
    }

    public void setEcosTypeRef(EntityRef ecosTypeRef) {
        this.ecosTypeRef = ecosTypeRef;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getAlfType() {
        return alfType;
    }

    public void setAlfType(String alfType) {
        this.alfType = alfType;
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

    public EntityRef getSectionRef() {
        return sectionRef;
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
        if (created == null) {
            this.created = Instant.EPOCH;
        } else {
            this.created = created;
        }
    }

    public Instant getModified() {
        return modified;
    }

    public void setModified(Instant modified) {
        if (modified == null) {
            this.modified = Instant.EPOCH;
        } else {
            this.modified = modified;
        }
    }

    public ProcDefRevDataState getDataState() {
        return dataState;
    }

    public void setDataState(ProcDefRevDataState dataState) {
        this.dataState = dataState;
    }
}
