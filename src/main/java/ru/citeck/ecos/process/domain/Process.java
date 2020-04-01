package ru.citeck.ecos.process.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "process")
@Data
public class Process {

    @Id
    private UUID id = UUID.randomUUID();

    private Integer tenant;

    private String record;

    @DBRef
    private UUID revisionId;

    @DBRef
    private UUID definitionRevId;

    private LocalDateTime created = LocalDateTime.now();

    private LocalDateTime modified;

    private boolean active;

}
