package ru.citeck.ecos.process.domain.proc.repo

import com.fasterxml.jackson.annotation.JsonValue
import com.mongodb.annotations.Immutable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.edata.EcosDataProcInstanceAdapter
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.records2.RecordConstants
import java.time.Instant
import java.util.*

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

    var migrated: Boolean? = false

    fun copyWithId(id: String): ProcessStateEntity {
        val copy = copy()
        copy.id = EntityUuid(0, UUID.fromString(id))
        return copy
    }

    fun copy(): ProcessStateEntity {
        val copy = ProcessStateEntity()
        copy.id = this.id
        copy.data = this.data
        copy.process = this.process
        copy.procDefRev = this.procDefRev
        copy.created = this.created
        copy.version = this.version
        return copy
    }

    @JsonValue
    fun getAsJson(): DataValue {
        val obj = DataValue.createObj()
        if (id != null) {
            obj["id"] = id?.id.toString()
        }
        return obj.set("data", data)
            .set("process", EcosDataProcInstanceAdapter.toRef(process))
            .set("procDefRev", EcosDataProcDefRevAdapter.toRef(procDefRev))
            .set("version", version)
            .set(RecordConstants.ATT_CREATED, created)
    }

    override fun toString(): String {
        return getAsJson().toString()
    }
}
