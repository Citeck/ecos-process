package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import jdk.nashorn.api.scripting.ScriptObjectMirror
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat
import org.camunda.spin.spi.DataFormatConfigurator
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskOutcome
import spinjar.com.fasterxml.jackson.core.JsonGenerator
import spinjar.com.fasterxml.jackson.core.JsonParser
import spinjar.com.fasterxml.jackson.databind.DeserializationContext
import spinjar.com.fasterxml.jackson.databind.JsonDeserializer
import spinjar.com.fasterxml.jackson.databind.JsonSerializer
import spinjar.com.fasterxml.jackson.databind.SerializerProvider
import spinjar.com.fasterxml.jackson.databind.module.SimpleModule

class CamundaJsonDataFormatConfiguration : DataFormatConfigurator<JacksonJsonDataFormat> {

    override fun getDataFormatClass(): Class<JacksonJsonDataFormat> {
        return JacksonJsonDataFormat::class.java
    }

    override fun configure(dataFormat: JacksonJsonDataFormat) {
        val mapper = dataFormat.objectMapper
        val module = SimpleModule()

        module.addSerializer(BpmnDataValue::class.java, BpmnDataValueJsonSerializer())
        module.addDeserializer(BpmnDataValue::class.java, BpmnDataValueJsonDeserializer())

        module.addSerializer(MLText::class.java, MLTextJsonSerializer())
        module.addDeserializer(MLText::class.java, MLTextJsonDeserializer())

        module.addSerializer(TaskOutcome::class.java, TaskOutcomeJsonSerializer())
        module.addDeserializer(TaskOutcome::class.java, TaskOutcomeJsonDeserializer())

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
