package ru.citeck.ecos.process.domain.procdef.records;

import ecos.com.fasterxml.jackson210.annotation.JsonProperty;
import ecos.com.fasterxml.jackson210.annotation.JsonValue;
import lombok.NoArgsConstructor;
import org.springframework.util.MimeTypeUtils;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto;
import ru.citeck.ecos.process.domain.procdef.eapps.casetemplate.CaseTemplateUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;

import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor
public class ProcDefRecord extends ProcDefWithDataDto {

    public ProcDefRecord(ProcDefWithDataDto model) {
        super(model);
    }

    @MetaAtt(".id")
    public String getRecId() {
        return getProcType() + "$" + super.getId();
    }

    public String getModuleId() {
        return getId();
    }

    public void setModuleId(String value) {
        setId(ProcDefRef.valueOf(value).getId());
    }

    @MetaAtt(".disp")
    public String getDisplayName() {
        return getId();
    }

    @MetaAtt(".type")
    public RecordRef getType() {
        return RecordRef.create("emodel", "type", "process-definition");
    }

    @JsonProperty("_content")
    public void setContent(List<ObjectData> content) {

        String base64Content = content.get(0).get("url", "");
        String filename = content.get(0).get("originalName", "");

        Pattern pattern = Pattern.compile("^data:(.+?);base64,(.+)$");
        Matcher matcher = pattern.matcher(base64Content);
        if (!matcher.find()) {
            throw new IllegalStateException("Incorrect data: " + base64Content);
        }

        String mimetype = matcher.group(1);
        String base64 = matcher.group(2);

        switch (mimetype) {
            case MimeTypeUtils.APPLICATION_JSON_VALUE:
                this.setFormat("json");
                break;
            case MimeTypeUtils.APPLICATION_XML_VALUE:
            case MimeTypeUtils.TEXT_XML_VALUE:
                this.setFormat("xml");
                break;
            default:
                throw new IllegalStateException("Unknown mimetype: " + mimetype);
        }

        //todo: type can be other than cmmn
        NewProcessDefDto procDefDto = CaseTemplateUtils.parseCmmn(filename, Base64.getDecoder().decode(base64));

        setId(procDefDto.getId());
        setProcType(procDefDto.getProcType());
        setFormat(procDefDto.getFormat());
        setAlfType(procDefDto.getAlfType());
        setEcosTypeRef(procDefDto.getEcosTypeRef());
        setData(procDefDto.getData());
    }

    @JsonValue
    @com.fasterxml.jackson.annotation.JsonValue
    public ProcDefWithDataDto toJson() {
        return new ProcDefWithDataDto(this);
    }
}
