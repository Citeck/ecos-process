package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat
import org.camunda.spin.spi.DataFormatConfigurator
import ru.citeck.ecos.bpmn.commons.values.BpmnDataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcomeConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcomeTheme
import spinjar.com.fasterxml.jackson.core.JsonGenerator
import spinjar.com.fasterxml.jackson.core.JsonParser
import spinjar.com.fasterxml.jackson.databind.DeserializationContext
import spinjar.com.fasterxml.jackson.databind.JsonDeserializer
import spinjar.com.fasterxml.jackson.databind.JsonSerializer
import spinjar.com.fasterxml.jackson.databind.SerializerProvider
import spinjar.com.fasterxml.jackson.databind.module.SimpleModule
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert.BpmnDataValue as BpmnDataValueDeprecated

class CamundaJsonDataFormatConfiguration : DataFormatConfigurator<JacksonJsonDataFormat> {

    override fun getDataFormatClass(): Class<JacksonJsonDataFormat> {
        return JacksonJsonDataFormat::class.java
    }

    @Suppress("DEPRECATION")
    override fun configure(dataFormat: JacksonJsonDataFormat) {
        val mapper = dataFormat.objectMapper
        val module = SimpleModule()

        module.addSerializer(BpmnDataValueDeprecated::class.java, BpmnDataValueBackwardCompatibilityJsonSerializer())
        module.addDeserializer(
            BpmnDataValueDeprecated::class.java,
            BpmnDataValueBackwardCompatibilityJsonDeserializer()
        )

        module.addSerializer(BpmnDataValue::class.java, BpmnDataValueJsonSerializer())
        module.addDeserializer(BpmnDataValue::class.java, BpmnDataValueJsonDeserializer())

        module.addSerializer(MLText::class.java, MLTextJsonSerializer())
        module.addDeserializer(MLText::class.java, MLTextJsonDeserializer())

        module.addSerializer(TaskOutcome::class.java, TaskOutcomeJsonSerializer())
        module.addDeserializer(TaskOutcome::class.java, TaskOutcomeJsonDeserializer())

        module.addSerializer(TaskOutcomeConfig::class.java, TaskOutcomeConfigJsonSerializer())
        module.addDeserializer(TaskOutcomeConfig::class.java, TaskOutcomeConfigJsonDeserializer())

        module.addSerializer(TaskOutcomeTheme::class.java, TaskOutcomeThemeJsonSerializer())
        module.addDeserializer(TaskOutcomeTheme::class.java, TaskOutcomeThemeJsonDeserializer())

        module.addSerializer(ScriptObjectMirror::class.java, ScriptObjectMirrorJsonSerializerProtection())
        module.addDeserializer(ScriptObjectMirror::class.java, ScriptObjectMirrorJsonDeserializerNull())

        mapper.registerModule(module)
    }
}

// Protect ScriptObjectMirror from saving to execution variable
class ScriptObjectMirrorJsonSerializerProtection : JsonSerializer<ScriptObjectMirror>() {

    override fun serialize(value: ScriptObjectMirror, gen: JsonGenerator, serializers: SerializerProvider) {
        error("Save ScriptObject to execution variable is not supported. Use DataValue instead.")
    }
}

// If ScriptObjectMirror already saved to execution variable, then deserialize it as null to prevent errors
class ScriptObjectMirrorJsonDeserializerNull : JsonDeserializer<ScriptObjectMirror>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ScriptObjectMirror? {
        return null
    }
}

@Suppress("DEPRECATION")
class BpmnDataValueBackwardCompatibilityJsonSerializer : JsonSerializer<BpmnDataValueDeprecated>() {

    override fun serialize(value: BpmnDataValueDeprecated, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeBinary(value.asBinary())
    }
}

@Suppress("DEPRECATION")
class BpmnDataValueBackwardCompatibilityJsonDeserializer : JsonDeserializer<BpmnDataValueDeprecated>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BpmnDataValueDeprecated {
        return BpmnDataValueDeprecated.create(p.binaryValue)
    }
}

class BpmnDataValueJsonSerializer : JsonSerializer<BpmnDataValue>() {

    override fun serialize(value: BpmnDataValue, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeBinary(value.asBinary())
    }
}

class BpmnDataValueJsonDeserializer : JsonDeserializer<BpmnDataValue>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BpmnDataValue {
        return BpmnDataValue.create(p.binaryValue)
    }
}

class MLTextJsonSerializer : JsonSerializer<MLText>() {

    override fun serialize(value: MLText, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeBinary(Json.mapper.toBytesNotNull(value))
    }
}

class MLTextJsonDeserializer : JsonDeserializer<MLText>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): MLText {
        return Json.mapper.readNotNull(p.binaryValue, MLText::class.java)
    }
}

class TaskOutcomeJsonSerializer : JsonSerializer<TaskOutcome>() {

    override fun serialize(value: TaskOutcome, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeBinary(Json.mapper.toBytesNotNull(value))
    }
}

class TaskOutcomeJsonDeserializer : JsonDeserializer<TaskOutcome>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TaskOutcome {
        return Json.mapper.readNotNull(p.binaryValue, TaskOutcome::class.java)
    }
}

class TaskOutcomeConfigJsonSerializer : JsonSerializer<TaskOutcomeConfig>() {

    override fun serialize(value: TaskOutcomeConfig, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeBinary(Json.mapper.toBytesNotNull(value))
    }
}

class TaskOutcomeConfigJsonDeserializer : JsonDeserializer<TaskOutcomeConfig>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TaskOutcomeConfig {
        return Json.mapper.readNotNull(p.binaryValue, TaskOutcomeConfig::class.java)
    }
}

class TaskOutcomeThemeJsonSerializer : JsonSerializer<TaskOutcomeTheme>() {

    override fun serialize(value: TaskOutcomeTheme, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeBinary(Json.mapper.toBytesNotNull(value))
    }
}

class TaskOutcomeThemeJsonDeserializer : JsonDeserializer<TaskOutcomeTheme>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): TaskOutcomeTheme {
        val stringValue = if (p.valueAsString == null || p.valueAsString.isEmpty()) {
            TaskOutcomeTheme.PRIMARY.name
        } else {
            p.valueAsString
        }

        return Json.mapper.readNotNull(stringValue, TaskOutcomeTheme::class.java)
    }
}
