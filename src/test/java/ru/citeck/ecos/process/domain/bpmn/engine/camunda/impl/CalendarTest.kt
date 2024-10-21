package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send.CalendarUtils
import java.time.Instant
import java.time.ZoneId

class CalendarTest {

    @ParameterizedTest
    @ValueSource(strings = ["+03:00", "Z", "UTC", "-6", "UTC+11","GMT-8"])
    fun `convert to ical Tz test`(userTimeZone: String) {
        val calendarUtils = CalendarUtils()

        val now = Instant.now();
        assertThat(
            calendarUtils.convertToICalTz(userTimeZone).toZoneId().rules.getOffset(now)
        )
            .isEqualTo(ZoneId.of(userTimeZone).rules.getOffset(now))
    }
}
