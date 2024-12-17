package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat
import org.camunda.spin.spi.DataFormatConfigurator
import org.graalvm.polyglot.Value
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

    companion object {
        private const val POLYGLOT_MAP_CLASS_NAME = "com.oracle.truffle.polyglot.PolyglotMap"
    }

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

        module.addSerializer(Value::class.java, ScriptObjectMirrorJsonSerializerProtection())
        module.addDeserializer(Value::class.java, ScriptObjectMirrorJsonDeserializerNull())

        module.addSerializer(Class.forName(POLYGLOT_MAP_CLASS_NAME), PolyglotMapSerializer())
        module.addDeserializer(Class.forName(POLYGLOT_MAP_CLASS_NAME), PolyglotMapDeserializer())

        mapper.registerModule(module)
    }
}

// Protect GraalVM value from saving to execution variable
class ScriptObjectMirrorJsonSerializerProtection : JsonSerializer<Value>() {

    override fun serialize(value: Value, gen: JsonGenerator, serializers: SerializerProvider) {
        error("Save GraalVM value to execution variable is not supported. Use DataValue instead.")
    }
}

// If GraalVM value already saved to execution variable, then deserialize it as null to prevent errors
class ScriptObjectMirrorJsonDeserializerNull : JsonDeserializer<Value>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Value? {
        return null
    }
}

// Fast fail to prevent saving unrecognized PolyglotMap to execution variable
class PolyglotMapSerializer : JsonSerializer<Any>() {
    override fun serialize(value: Any, gen: JsonGenerator, serializers: SerializerProvider) {
        error(
            "Save PolyglotMap to execution variable is not supported. " +
                "If you trying to save some structure - use DataValue instead. " +
                "If you trying to save js Date - save as ISO string."
        )
    }
}

// If PolyglotMap already saved to execution variable, then deserialize it as null to prevent errors
class PolyglotMapDeserializer<T> : JsonDeserializer<T>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T? {
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
