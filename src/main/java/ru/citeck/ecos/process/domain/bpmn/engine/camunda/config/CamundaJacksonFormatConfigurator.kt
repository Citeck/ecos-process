package ru.citeck.ecos.process.domain.bpmn.engine.camunda.config

import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat
import org.camunda.spin.spi.DataFormatConfigurator
import org.springframework.stereotype.Component

@Component
class CamundaJacksonFormatConfigurator : DataFormatConfigurator<JacksonJsonDataFormat> {

    override fun getDataFormatClass(): Class<JacksonJsonDataFormat> {
        return JacksonJsonDataFormat::class.java
    }

    override fun configure(dataFormat: JacksonJsonDataFormat) {
       /* val mapper: ObjectMapper = dataFormat.objectMapper as ObjectMapper

        val registeredModuleIds = mapper.registeredModuleIds

        val javaTimeModule = JavaTimeModule()
        mapper.registerModule(javaTimeModule)*/
    }
}
