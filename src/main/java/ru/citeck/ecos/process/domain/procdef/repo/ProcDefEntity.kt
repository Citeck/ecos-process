package ru.citeck.ecos.process.domain.procdef.repo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import java.time.Instant

@Document(collection = "process_def")
@CompoundIndexes(
    CompoundIndex(name = "proc_def_tnt_proc_type_ext_id_idx", def = "{'id.tnt': 1, 'procType' : 1, 'extId': 1}"),
    CompoundIndex(name = "proc_def_tnt_proc_type_ecos_type_idx", def = "{'id.tnt': 1, 'procType' : 1, 'ecosTypeRef': 1}"),
    CompoundIndex(name = "proc_def_tnt_proc_type_alf_type_idx", def = "{'id.tnt': 1, 'procType' : 1, 'alfType': 1}")
)
class ProcDefEntity {

    @Id
    var id: EntityUuid? = null

    /**
     * Engine type (cmmn)
     */
    var procType: String? = null
    var name: String? = null
    var extId: String? = null
    var ecosTypeRef: String? = null
    var formRef: String? = null
    var alfType: String? = null
    var created: Instant? = null
    var modified: Instant? = null
    var enabled: Boolean? = null
    var autoStartEnabled: Boolean? = null
    var sectionRef: String? = null

    @DBRef
    var lastRev: ProcDefRevEntity? = null
}
