package ru.citeck.ecos.process.domain.procdef.repo

import com.fasterxml.jackson.annotation.JsonValue
import com.mongodb.annotations.Immutable
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.DBRef
import org.springframework.data.mongodb.core.mapping.Document
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefAdapter
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import java.time.Instant
import java.util.*

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

    fun copyWithId(id: String): ProcDefRevEntity {
        val copy = copy()
        copy.id = EntityUuid(0, UUID.fromString(id))
        return copy
    }

    fun copy(): ProcDefRevEntity {
        val copy = ProcDefRevEntity()
        copy.id = id
        copy.format = format
        copy.data = data
        copy.processDef = processDef
        copy.created = created
        copy.createdBy = createdBy
        copy.deploymentId = deploymentId
        copy.dataState = dataState
        copy.comment = comment
        copy.version = version
        copy.prevRev = prevRev
        copy.image = image
        return copy
    }

    @JsonValue
    fun getAsJson(): DataValue {
        return DataValue.createObj()
            .set("id", id?.id?.toString())
            .set("format", format)
            .set("data", data)
            .set("processDef", EcosDataProcDefAdapter.toRef(processDef))
            .set("_created", created)
            .set("_creator", createdBy)
            .set("deploymentId", deploymentId)
            .set("dataState", dataState)
            .set("comment", comment)
            .set("version", version)
            .set("prevRev", EcosDataProcDefRevAdapter.toRef(prevRev))
            .set("image", image)
    }

    override fun toString(): String {
        return getAsJson().toString()
    }
}
