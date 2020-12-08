package ru.citeck.ecos.process.domain.proc.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.citeck.ecos.process.domain.common.entity.EntityUuid;

import java.time.Instant;

@Document(collection = "process_instance")
@Getter @Setter
@NoArgsConstructor
@EqualsAndHashCode
public class ProcessInstanceEntity {

    @Id
    private EntityUuid id;

    private String procType;

    private String recordRef;

    @DBRef
    private ProcessStateEntity state;

    private Instant created;

    private Instant modified;
}