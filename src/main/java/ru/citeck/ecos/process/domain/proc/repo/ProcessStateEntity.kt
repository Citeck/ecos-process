package ru.citeck.ecos.process.domain.proc.repo

import com.mongodb.annotations.Immutable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import java.time.Instant

@Immutable
@Document(collection = "process_state")
class ProcessStateEntity {

    @Id
    var id: EntityUuid? = null
    var data: ByteArray? = null

    @DBRef
    var process: ProcessInstanceEntity? = null

    @DBRef
    var procDefRev: ProcDefRevEntity? = null
    var created: Instant = Instant.now()
    var version = 0
}
