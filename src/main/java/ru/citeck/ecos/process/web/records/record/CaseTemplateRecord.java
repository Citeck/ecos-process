package ru.citeck.ecos.process.web.records.record;

import lombok.Data;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.util.Base64;

@Data
public class CaseTemplateRecord implements MetaValue {

    private final String id;
    private final RecordRef ecosTypeRef;
    private final byte[] xmlContent;

    public CaseTemplateRecord(CaseTemplateEntity entity) {
        this.ecosTypeRef = entity.getEcosTypeRef();
        this.xmlContent = entity.getXmlContent();
        this.id = entity.getId();
    }

    @Override
    public String getDisplayName() {
        return id;
    }

    @Override
    public Object getAttribute(String fieldName, MetaField field) {
        switch (fieldName) {
            case "ecosTypeRef":
                return ecosTypeRef.toString();
            case "xmlContent":
                return Base64.getEncoder().encodeToString(xmlContent);
        }
        return null;
    }
}
