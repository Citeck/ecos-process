package ru.citeck.ecos.process.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "process_def")
@Getter @Setter
@NoArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "proc_def_proc_type_ext_id_idx", def = "{'tenant': 1, 'procType' : 1, 'extId': 1}"),
    @CompoundIndex(name = "proc_def_proc_type_ecos_type_idx", def = "{'tenant': 1, 'procType' : 1, 'ecosTypeRef': 1}"),
    @CompoundIndex(name = "proc_def_proc_type_alf_type_idx", def = "{'tenant': 1, 'procType' : 1, 'alfType': 1}"),
})
public class ProcessDefEntity {

    @Id
    private EntityUuid id;

    private int tenant;

    /**
     * Engine type (cmmn)
     */
    private String procType;

    private String extId;

    private String ecosTypeRef;

    private String alfType;

    private LocalDateTime created;

    private LocalDateTime modified;

    @DBRef
    private ProcessDefRevEntity lastRev;
}
