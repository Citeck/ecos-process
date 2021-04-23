package ru.citeck.ecos.process.domain.proc.repo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import java.time.Instant

@Document(collection = "process_instance")
class ProcessInstanceEntity {

    @Id
    var id: EntityUuid? = null
    var procType: String? = null
    var recordRef: String? = null

    @DBRef
    var state: ProcessStateEntity? = null
    var created: Instant? = null
    var modified: Instant? = null

    override fun equals(other: Any?): Boolean {

        if (this === other) {
            return true
        }
        if (javaClass != other?.javaClass) {
            return false
        }
        other as ProcessInstanceEntity

        if (id != other.id) {
            return false
        }
        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}
