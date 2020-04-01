package ru.citeck.ecos.process.web.records.record;

import lombok.Data;
import ru.citeck.ecos.process.dto.CaseTemplateDto;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Base64;

@Data
public class CaseTemplateRecord implements MetaValue {

    private final String id;
    private final RecordRef typeRef;
    private final byte[] xmlContent;

    public CaseTemplateRecord(CaseTemplateDto dto) {
        this.typeRef = dto.getTypeRef();
        this.xmlContent = dto.getXmlContent();
        this.id = dto.getId();
    }

    @Override
    public String getDisplayName() {
        return id;
    }

    @Override
    public Object getAttribute(String fieldName, MetaField field) {
        switch (fieldName) {
            case "typeRef":
                return typeRef.toString();
            case "xmlContent":
                return Base64.getEncoder().encodeToString(xmlContent);
        }
        return null;
    }
}
