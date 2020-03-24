package ru.citeck.ecos.process.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "process_definitions")
@Data
public class ProcessDefinition {

    @Id
    private String id;
    private Integer tenant;
    private LocalDateTime created = LocalDateTime.now();
    private LocalDateTime modified;
    private UUID revisionId;
}
