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

    @DBRef(lazy = true)
    var processDef: ProcDefEntity? = null

    var created: Instant = Instant.now()

    var createdBy: String? = null

    var deploymentId: String? = null

    var dataState: String? = null

    var comment: String? = null

    var version = 0

    @DBRef(lazy = true)
    var prevRev: ProcDefRevEntity? = null

    var image: ByteArray? = null
}
