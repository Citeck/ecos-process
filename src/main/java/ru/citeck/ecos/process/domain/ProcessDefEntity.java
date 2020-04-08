package ru.citeck.ecos.process.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "process_def")
@Getter @Setter
@NoArgsConstructor
public class ProcessDefEntity {

    @Id
    private EntityUuid id;

    /**
     * Engine type (cmmn)
     */
    private String procType;

    private String extId;

    private String ecosTypeRef;

    private LocalDateTime created;

    private LocalDateTime modified;

    @DBRef
    private ProcessDefRevEntity lastRev;
}
