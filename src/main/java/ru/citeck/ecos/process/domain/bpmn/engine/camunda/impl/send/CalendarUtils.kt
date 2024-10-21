package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.send

import io.github.oshai.kotlinlogging.KotlinLogging
import net.fortuna.ical4j.model.TimeZone
import net.fortuna.ical4j.model.TimeZoneRegistryFactory
import net.fortuna.ical4j.util.TimeZones
import org.springframework.stereotype.Component

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@Component
class CalendarUtils {

    companion object {
        private val log = KotlinLogging.logger {}
        private val timeZoneRegistry = TimeZoneRegistryFactory.getInstance().createRegistry()
    }

    fun convertToICalTz(userTimeZone: String?): TimeZone {
        if (userTimeZone.isNullOrBlank()) {
            log.error { "Invalid timeZone: '$userTimeZone'" }
            return timeZoneRegistry.getTimeZone(TimeZones.UTC_ID);
        }

        return timeZoneRegistry.getTimeZone(convertToTz(userTimeZone));
    }

    fun convertToTz(userTimeZone: String): String {
        val tzOffset = try {
            ZoneId.of(userTimeZone).rules.getOffset(Instant.now())
        } catch (e: RuntimeException) {
            log.error(e) { "Invalid timeZone: '$userTimeZone'" }
            ZoneOffset.UTC
        }
        val offsetHours = tzOffset.totalSeconds / 3600
        return if (offsetHours == 0) {
            TimeZones.UTC_ID
        } else {
            String.format("Etc/GMT%+d", -offsetHours)
        }
    }
}

