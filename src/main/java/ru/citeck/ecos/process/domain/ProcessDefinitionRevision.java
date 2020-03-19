package ru.citeck.ecos.process.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "process_definitions_rev")
@Getter
public class ProcessDefinitionRevision {

    @Id
    private UUID id = UUID.randomUUID();

    private byte[] data;

    @DBRef
    private String processDefinitionId;

    private LocalDateTime created = LocalDateTime.now();

    /**
     * Setter only for autoincrement value of revision version
     */
    @Setter
    private Integer version = -1;

    public ProcessDefinitionRevision(byte[] data, String processDefinitionId) {
        this.data = data;
        this.processDefinitionId = processDefinitionId;
    }
}
