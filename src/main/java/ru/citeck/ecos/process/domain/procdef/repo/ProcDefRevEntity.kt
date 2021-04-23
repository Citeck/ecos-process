package ru.citeck.ecos.process.domain.procdef.repo

import com.mongodb.annotations.Immutable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import java.time.Instant

@Immutable
@Document(collection = "process_def_rev")
class ProcDefRevEntity {

    @Id
    var id: EntityUuid? = null
    var format: String? = null

    var data: ByteArray? = null

    @DBRef
    var processDef: ProcDefEntity? = null

    var created: Instant = Instant.now()
    var version = 0

    @DBRef
    var prevRev: ProcDefRevEntity? = null
}
