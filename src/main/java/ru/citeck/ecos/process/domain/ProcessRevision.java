package ru.citeck.ecos.process.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Document(collection = "process_revision")
@Getter
@NoArgsConstructor
public class ProcessRevision {

    @Id
    private UUID id = UUID.randomUUID();

    @Setter
    private byte[] data;

    @DBRef
    @Setter
    private Process process;

    private LocalDateTime created = LocalDateTime.now();

    /**
     * Setter only for autoincrement value of revision version
     */
    @Setter
    private Integer version = -1;

    public ProcessRevision(byte[] data, Process process) {
        this.data = data;
        this.process = process;
    }
}
