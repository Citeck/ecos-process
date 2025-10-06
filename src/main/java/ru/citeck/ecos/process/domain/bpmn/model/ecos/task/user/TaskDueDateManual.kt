package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.time.Duration

data class TaskDueDateManual(
    val durationType: DurationType? = null,
    val duration: Duration? = null,
    val workingDays: Int? = null,
    val workingSchedule: EntityRef = "emodel/working-schedule@DEFAULT".toEntityRef()
) {

    fun validate(elementId: String) {
        if (workingDays != null && workingDays <= 0) {
            throw EcosBpmnElementDefinitionException(elementId, "Working days must be positive")
        }

        if (duration != null && duration.toMillis() <= 0) {
            throw EcosBpmnElementDefinitionException(elementId, "Duration must be positive")
        }

        if (durationType == DurationType.CALENDAR && duration == null) {
            throw EcosBpmnElementDefinitionException(elementId, "Duration must be set for calendar duration type")
        }

        if (durationType == DurationType.BUSINESS && duration == null && workingDays == null) {
            throw EcosBpmnElementDefinitionException(
                elementId,
                "Duration or working days must be set for business duration type"
            )
        }

        if (duration != null && workingDays != null) {
            throw EcosBpmnElementDefinitionException(
                elementId,
                "Only one of the fields should be set of duration and working days"
            )
        }
    }
}

@JsonDeserialize(using = DurationTypeJsonDeserializer::class)
enum class DurationType {
    CALENDAR,
    BUSINESS
}

class DurationTypeJsonDeserializer : JsonDeserializer<DurationType>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DurationType? {
        if (p.valueAsString.isNullOrEmpty()) {
            return null
        }

        return DurationType.valueOf(p.valueAsString)
    }
}
