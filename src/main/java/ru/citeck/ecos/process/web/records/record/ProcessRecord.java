package ru.citeck.ecos.process.web.records.record;

import lombok.Data;
import ru.citeck.ecos.process.dto.ProcessDto;
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.graphql.meta.value.MetaValue;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ProcessRecord implements MetaValue {

    private UUID id;

    private Integer tenant;

    private String record;

    private UUID revisionId;

    private UUID definitionRevId;

    private LocalDateTime created;

    private LocalDateTime modified;

    private boolean active;

    public ProcessRecord(ProcessDto dto) {
        this.id = dto.getId();
        this.tenant = dto.getTenant();
        this.revisionId = dto.getRevisionId();
        this.definitionRevId = dto.getDefinitionRevId();
        this.created = dto.getCreated();
        this.modified = dto.getModified();
        this.active = dto.isActive();
        this.record = dto.getRecord();
    }

    @Override
    public String getId() {
        return id.toString();
    }

    @Override
    public String getDisplayName() {
        return id.toString();
    }

    @Override
    public Object getAttribute(String name, MetaField field) {
        switch (name) {
            case "id":
                return this.id;
            case "tenant":
                return this.tenant;
            case "record":
                return this.record;
            case "revisionId":
                return this.revisionId;
            case "definitionRevId":
                return this.definitionRevId;
            case "created":
                return this.created;
            case "modified":
                return this.modified;
            case "active":
                return this.active;
        }
        return null;
    }
}
