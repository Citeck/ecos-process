package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import ru.citeck.ecos.commons.data.MLText

data class TaskOutcome(
    val id: String,
    val name: MLText,
    val config: TaskOutcomeConfig = TaskOutcomeConfig()
)

data class TaskOutcomeConfig(
    val theme: TaskOutcomeTheme = TaskOutcomeTheme.PRIMARY,
)

@JsonDeserialize(using = TaskOutcomeThemeJsonDeserializer::class)
enum class TaskOutcomeTheme {
    DEFAULT,
    PRIMARY,
    INFO,
    SUCCESS,
    DANGER,
    WARNING
}

class TaskOutcomeThemeJsonDeserializer : JsonDeserializer<TaskOutcomeTheme>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TaskOutcomeTheme {
        val stringValue = if (p.valueAsString == null || p.valueAsString.isEmpty()) {
            TaskOutcomeTheme.PRIMARY.name
        } else {
            p.valueAsString
        }

        return TaskOutcomeTheme.valueOf(stringValue)
    }
}
