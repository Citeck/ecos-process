package ru.citeck.ecos.process.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "process_def")
@Getter @Setter
@NoArgsConstructor
@CompoundIndexes({
    @CompoundIndex(name = "proc_def_tnt_proc_type_ext_id_idx", def = "{'id.tnt': 1, 'procType' : 1, 'extId': 1}"),
    @CompoundIndex(name = "proc_def_tnt_proc_type_ecos_type_idx", def = "{'id.tnt': 1, 'procType' : 1, 'ecosTypeRef': 1}"),
    @CompoundIndex(name = "proc_def_tnt_proc_type_alf_type_idx", def = "{'id.tnt': 1, 'procType' : 1, 'alfType': 1}"),
})
public class ProcessDefEntity {

    @Id
    private EntityUuid id;

    /**
     * Engine type (cmmn)
     */
    private String procType;

    private String extId;

    private String ecosTypeRef;

    private String alfType;

    private Instant created;

    private Instant modified;

    @DBRef
    private ProcessDefRevEntity lastRev;
}
