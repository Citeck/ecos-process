package ru.citeck.ecos.process.domain.procdef.entity;

import com.mongodb.annotations.Immutable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import ru.citeck.ecos.process.domain.common.entity.EntityUuid;

import java.time.Instant;

@Document(collection = "process_def_rev")
@Getter @Setter
@NoArgsConstructor
@Immutable
public class ProcDefRevEntity {

    @Id
    private EntityUuid id;

    private String format;

    private byte[] data;

    @DBRef
    private ProcDefEntity processDef;

    private Instant created = Instant.now();

    private int version;

    @DBRef
    private ProcDefRevEntity prevRev;
}
