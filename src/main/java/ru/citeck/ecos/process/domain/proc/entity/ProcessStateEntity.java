package ru.citeck.ecos.process.domain.proc.entity;

import com.mongodb.annotations.Immutable;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;

import java.time.Instant;

@Document(collection = "process_state")
@Getter
@Setter
@Immutable
public class ProcessStateEntity {

    @Id
    private EntityUuid id;

    private byte[] data;

    @DBRef
    private ProcessInstanceEntity process;

    @DBRef
    private ProcDefRevEntity procDefRev;

    private Instant created = Instant.now();

    private int version;
}
