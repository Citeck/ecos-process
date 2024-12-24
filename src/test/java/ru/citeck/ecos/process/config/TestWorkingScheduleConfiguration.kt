package ru.citeck.ecos.process.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.citeck.ecos.wkgsch.lib.calendar.WorkingCalendarImpl
import ru.citeck.ecos.wkgsch.lib.schedule.dto.WorkingDayTime
import ru.citeck.ecos.wkgsch.lib.schedule.type.weekly.WeeklyWorkingSchedule
import ru.citeck.ecos.wkgsch.lib.schedule.type.weekly.WeeklyWorkingScheduleConfig
import java.time.ZoneOffset

@Configuration
class TestWorkingScheduleConfiguration {

    @Bean
    fun testWeaklyWorkingSchedule(): WeeklyWorkingSchedule {
        return WeeklyWorkingSchedule(
            WorkingCalendarImpl(emptyMap()),
            WeeklyWorkingScheduleConfig(
                workingDayStart = WorkingDayTime.valueOf("09:00"),
                workingDayEnd = WorkingDayTime.valueOf("17:00"),
                workingDayTimeZone = ZoneOffset.of("+3")
            )
        )
    }
}
