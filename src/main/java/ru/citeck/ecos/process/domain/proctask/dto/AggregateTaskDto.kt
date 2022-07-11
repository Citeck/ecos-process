package ru.citeck.ecos.process.domain.proctask.dto

import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import java.util.*

data class AggregateTaskDto(
    var id: String? = "",

    @AttName("...")
    var aggregationRef: RecordRef? = RecordRef.EMPTY,

    @AttName("_created")
    val createTime: Date? = null
)
