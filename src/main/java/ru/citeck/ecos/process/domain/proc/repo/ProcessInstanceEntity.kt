package ru.citeck.ecos.process.domain.proc.repo

import com.fasterxml.jackson.annotation.JsonValue
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcStateAdapter
import ru.citeck.ecos.records2.RecordConstants
import java.time.Instant
import java.util.*

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

    fun copyWithId(id: String): ProcessInstanceEntity {
        val copy = copy()
        copy.id = EntityUuid(0, UUID.fromString(id))
        return copy
    }

    fun copy(): ProcessInstanceEntity {
        val copy = ProcessInstanceEntity()
        copy.id = this.id
        copy.procType = this.procType
        copy.recordRef = this.recordRef
        copy.state = this.state
        copy.created = this.created
        copy.modified = this.modified
        return copy
    }

    @JsonValue
    fun getAsJson(): DataValue {
        val obj = DataValue.createObj()
        if (id != null) {
            obj["id"] = id?.id.toString()
        }
        return obj.set("procType", procType)
            .set("recordRef", recordRef)
            .set("state", EcosDataProcStateAdapter.toRef(state))
            .set(RecordConstants.ATT_CREATED, created)
            .set(RecordConstants.ATT_MODIFIED, modified)
    }

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

    override fun toString(): String {
        return getAsJson().toString()
    }
}
