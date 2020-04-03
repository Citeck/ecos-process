package ru.citeck.ecos.process.domain;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "process_definition")
@Getter
@NoArgsConstructor
public class ProcessDefinition {

    @Id
    @Setter
    private String id;

    @Setter
    private Integer tenant;

    private LocalDateTime created = LocalDateTime.now();

    @Setter
    private LocalDateTime modified;

    @DBRef
    @Setter
    private ProcessDefinitionRevision definitionRevision;

    public ProcessDefinition(Integer tenant, ProcessDefinitionRevision definitionRevision) {
        this.tenant = tenant;
        this.definitionRevision = definitionRevision;
    }
}
