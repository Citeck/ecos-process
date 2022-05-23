package ru.citeck.ecos.process.domain.procdef.dto;

import lombok.Data;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;

import javax.validation.constraints.NotNull;
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
    private RecordRef ecosTypeRef;

    @NotNull
    private String alfType;

    private RecordRef formRef;

    @NotNull
    private Boolean enabled;

    @NotNull
    private Boolean autoStartEnabled;

    public ProcDefDto(@NotNull String id,
                      MLText name,
                      @NotNull String procType,
                      @NotNull String format,
                      @NotNull UUID revisionId,
                      @NotNull RecordRef ecosTypeRef,
                      @NotNull String alfType,
                      RecordRef formRef,
                      Boolean enabled,
                      Boolean autoStartEnabled) {
        this.id = id;
        this.name = name;
        this.procType = procType;
        this.format = format;
        this.revisionId = revisionId;
        this.ecosTypeRef = ecosTypeRef;
        this.alfType = alfType;
        this.formRef = formRef;
        this.enabled = enabled;
        this.autoStartEnabled = autoStartEnabled;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public RecordRef getFormRef() {
        return formRef;
    }

    public void setFormRef(RecordRef formRef) {
        this.formRef = formRef;
    }

    public Boolean getAutoStartEnabled() {
        return autoStartEnabled;
    }

    public void setAutoStartEnabled(Boolean autoStartEnabled) {
        this.autoStartEnabled = autoStartEnabled;
    }
}
