package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send.CalendarUtils
import java.time.Instant
import java.time.ZoneId

class CalendarTest {

    @ParameterizedTest
    @ValueSource(strings = ["+03:00", "Z", "UTC", "-6", "UTC+11", "GMT-8", "Etc/GMT+5"])
    fun `convert to ical Tz test`(userTimeZone: String) {
        val now = Instant.EPOCH
        assertThat(
            CalendarUtils.convertToICalTz(userTimeZone).toZoneId().rules.getOffset(now)
        )
            .isEqualTo(ZoneId.of(userTimeZone).rules.getOffset(now))
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = ["123" , "", "GMT+3,GMT+4", "UTC - 4", "Etc/GMT+30"])
    fun `convert to ical Tz test error timezone`(userTimeZone: String?) {
        val now = Instant.EPOCH
        assertThat(
            CalendarUtils.convertToICalTz(userTimeZone).toZoneId().rules.getOffset(now)
        )
            .isEqualTo(ZoneId.of("UTC").rules.getOffset(now))
    }
}
