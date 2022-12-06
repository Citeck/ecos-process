package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat
import org.camunda.spin.spi.DataFormatConfigurator
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import spinjar.com.fasterxml.jackson.core.JsonGenerator
import spinjar.com.fasterxml.jackson.core.JsonParser
import spinjar.com.fasterxml.jackson.databind.DeserializationContext
import spinjar.com.fasterxml.jackson.databind.JsonDeserializer
import spinjar.com.fasterxml.jackson.databind.JsonSerializer
import spinjar.com.fasterxml.jackson.databind.SerializerProvider
import spinjar.com.fasterxml.jackson.databind.module.SimpleModule

class CamundaDataValueFormatConfiguration : DataFormatConfigurator<JacksonJsonDataFormat> {

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

        mapper.registerModule(module)
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
