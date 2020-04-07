package ru.citeck.ecos.process.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "process")
@Getter
@NoArgsConstructor
@EqualsAndHashCode
public class Process {

    @Id
    @Setter
    private UUID id = UUID.randomUUID();

    @Setter
    private Integer tenant;

    @Setter
    private String record;

    @DBRef
    @Setter
    private ProcessRevision revision;

    @DBRef
    @Setter
    private ProcessDefinitionRevision definitionRevision;

    private LocalDateTime created = LocalDateTime.now();

    @Setter
    private LocalDateTime modified = LocalDateTime.now();

    @Setter
    private boolean active;

    public Process(Integer tenant, String record, ProcessRevision revision, ProcessDefinitionRevision definitionRev) {
        this.tenant = tenant;
        this.record = record;
        this.revision = revision;
        this.definitionRevision = definitionRev;
    }
}
