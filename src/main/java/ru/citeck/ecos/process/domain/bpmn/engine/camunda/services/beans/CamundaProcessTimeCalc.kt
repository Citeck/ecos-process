package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.springframework.stereotype.Component
import ru.citeck.ecos.bpmn.commons.values.Duration
import ru.citeck.ecos.bpmn.commons.values.Time
import ru.citeck.ecos.wkgsch.lib.schedule.WorkingScheduleService
import java.time.Instant

@Component("Time")
class CamundaProcessTimeCalc(
    private val workingScheduleService: WorkingScheduleService,
    private val timeNowProvider: TimeNowProvider
) : CamundaProcessEngineService {

    companion object {
        private const val DEFAULT_SCHEDULE_ID = "DEFAULT"
    }

    override fun getKey(): String {
        return "Time"
    }

    fun now(): Time {
        return timeNowProvider.now()
    }

    fun of(instant: Instant): Time {
        return Time.of(instant)
    }

    fun of(isoString: String): Time {
        return Time.of(isoString)
    }

    fun ofEpochMilli(epochMillis: Long): Time {
        return Time.of(Instant.ofEpochMilli(epochMillis))
    }

    fun plus(time: Time, duration: String): Time {
        return time.plus(duration)
    }

    fun minus(time: Time, duration: String): Time {
        return time.minus(duration)
    }

    fun durationBetween(start: Time, end: Time): Duration {
        return Duration.ofMillis(end.instant.toEpochMilli() - start.instant.toEpochMilli())
    }

    fun nowPlus(duration: String): Time {
        return timeNowProvider.now().plus(duration)
    }

    fun nowMinus(duration: String): Time {
        return timeNowProvider.now().minus(duration)
    }

    fun plusWorkingTime(time: Time, duration: String): Time {
        return plusWorkingTime(time, duration, DEFAULT_SCHEDULE_ID)
    }

    fun plusWorkingTime(time: Time, duration: String, scheduleId: String): Time {
        val schedule = workingScheduleService.getScheduleById(scheduleId)
        val durationEntity = Duration.of(duration)

        return Time.of(schedule.addWorkingTime(time.instant, durationEntity.toJavaDuration()))
    }

    fun nowPlusWorkingTime(duration: String): Time {
        return nowPlusWorkingTime(duration, DEFAULT_SCHEDULE_ID)
    }

    fun nowPlusWorkingTime(duration: String, scheduleId: String): Time {
        val schedule = workingScheduleService.getScheduleById(scheduleId)
        val durationEntity = Duration.of(duration)
        val now = timeNowProvider.now()

        return Time.of(schedule.addWorkingTime(now.instant, durationEntity.toJavaDuration()))
    }

    // Current implementation of schedule.addWorkingDays works incorrectly. Using is not recommended.
    // See https://jira.citeck.ru/browse/ECOSENT-3164
    @Deprecated(message = "Not stable", replaceWith = ReplaceWith("nowPlusWorkingTime"))
    fun plusWorkingDays(time: Time, days: Int): Time {
        return plusWorkingDays(time, days, DEFAULT_SCHEDULE_ID)
    }

    @Deprecated(message = "Not stable", replaceWith = ReplaceWith("nowPlusWorkingTime"))
    fun plusWorkingDays(time: Time, days: Int, scheduleId: String): Time {
        val schedule = workingScheduleService.getScheduleById(scheduleId)
        return Time.of(schedule.addWorkingDays(time.instant, days))
    }

    @Deprecated(message = "Not stable", replaceWith = ReplaceWith("nowPlusWorkingTime"))
    fun nowPlusWorkingDays(days: Int): Time {
        return nowPlusWorkingDays(days, DEFAULT_SCHEDULE_ID)
    }

    @Deprecated(message = "Not stable", replaceWith = ReplaceWith("nowPlusWorkingTime"))
    fun nowPlusWorkingDays(days: Int, scheduleId: String): Time {
        val schedule = workingScheduleService.getScheduleById(scheduleId)

        val now = timeNowProvider.now()
        return Time.of(schedule.addWorkingDays(now.instant, days))
    }
}

// Hack for testing
@Component
class TimeNowProvider {

    fun now(): Time {
        return Time.now()
    }
}
