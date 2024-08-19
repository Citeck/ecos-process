package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

data class AggregateTaskDto(
    var id: String? = "",

    @AttName("...")
    var aggregationRef: EntityRef? = EntityRef.EMPTY,

    @AttName("_created")
    val createTime: Date? = null
)
