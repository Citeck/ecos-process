package ru.citeck.ecos.process.domain.proc.dto;

import lombok.Data;
import lombok.ToString;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.records2.RecordRef;

@Data
@ToString(exclude = { "data" })
public class NewProcessDefDto {

    private String id;
    private MLText name;
    private String procType;
    private String format;
    private String alfType;
    private RecordRef ecosTypeRef;
    private RecordRef formRef;
    private byte[] data;

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

    public String getAlfType() {
        return alfType;
    }

    public void setAlfType(String alfType) {
        this.alfType = alfType;
    }

    public RecordRef getEcosTypeRef() {
        return ecosTypeRef;
    }

    public void setEcosTypeRef(RecordRef ecosTypeRef) {
        this.ecosTypeRef = ecosTypeRef;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public RecordRef getFormRef() {
        return formRef;
    }

    public void setFormRef(RecordRef formRef) {
        this.formRef = formRef;
    }
}
