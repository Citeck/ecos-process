package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.convert

import org.assertj.core.api.Assertions.assertThat
import org.camunda.spin.impl.json.jackson.format.JacksonJsonDataFormat
import org.junit.jupiter.api.Test
import spinjar.com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * Spin's `JacksonJsonDataFormat` is built on a bare `ObjectMapper` with no JSR-310 module, so every `java.time`
 * type reaching a process variable has to be registered here or camunda fails the command. That is what an
 * [Instant] did before `InstantJsonSerializer`.
 */
class CamundaJsonDataFormatConfigurationTest {

    private fun configuredMapper(): ObjectMapper {
        val dataFormat = JacksonJsonDataFormat("application/json")
        CamundaJsonDataFormatConfiguration().configure(dataFormat)
        return dataFormat.objectMapper
    }

    @Test
    fun `an Instant is written as an ISO-8601 string and read back`() {
        val mapper = configuredMapper()

        val json = mapper.writeValueAsString(Instant.EPOCH)

        assertThat(json).isEqualTo("\"1970-01-01T00:00:00Z\"")
        assertThat(mapper.readValue(json, Instant::class.java)).isEqualTo(Instant.EPOCH)
    }

    @Test
    fun `a stored js array reads back as a plain list`() {
        // spin stores the variable's type as PolyglotList, so reading it back used to ask jackson to rebuild one
        // and silently yield null. Nothing needs a PolyglotList: read the stored json as a plain list.
        val polyglotList = Class.forName("com.oracle.truffle.polyglot.PolyglotList")

        val readBack = configuredMapper().readValue("[1,2,3]", polyglotList)

        assertThat(readBack).isEqualTo(listOf(1, 2, 3))
    }
}
