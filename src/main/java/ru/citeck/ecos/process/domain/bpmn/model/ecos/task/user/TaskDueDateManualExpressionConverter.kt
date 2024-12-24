package ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user

import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Duration

const val WORKING_SCHEDULE_SOURCE_ID = "working-schedule"

fun TaskDueDateManual.toExpression(): String {

    return when {
        durationType == DurationType.CALENDAR -> "\${Time.nowPlus(\"${duration}\").toIsoString()}"
        durationType == DurationType.BUSINESS && duration != null -> {
            "\${Time.nowPlusWorkingTime(\"${duration}\", \"${workingSchedule.getLocalId()}\").toIsoString()}"
        }

        durationType == DurationType.BUSINESS && workingDays != null -> {
            "\${Time.nowPlusWorkingDays($workingDays, \"${workingSchedule.getLocalId()}\").toIsoString()}"
        }

        else -> error("Invalid TaskDueDateManual configuration: $this")
    }
}

fun String.toTaskDueDateManual(): TaskDueDateManual {

    val calendarRegex = """\$\{Time\.nowPlus\("([^"]+)"\)\.toIsoString\(\)}""".toRegex()
    val businessTimeRegex = """\$\{Time\.nowPlusWorkingTime\("([^"]+)", "([^"]+)"\)\.toIsoString\(\)}""".toRegex()
    val businessDaysRegex = """\$\{Time\.nowPlusWorkingDays\((\d+), "([^"]+)"\)\.toIsoString\(\)}""".toRegex()

    val dueDateManual = when {
        calendarRegex.matches(this) -> {
            val matchResult = calendarRegex.find(this)!!
            TaskDueDateManual(
                durationType = DurationType.CALENDAR,
                duration = Duration.parse(matchResult.groupValues[1]),
                workingDays = null,
                workingSchedule = EntityRef.EMPTY
            )
        }

        businessTimeRegex.matches(this) -> {
            val matchResult = businessTimeRegex.find(this)!!
            TaskDueDateManual(
                durationType = DurationType.BUSINESS,
                duration = Duration.parse(matchResult.groupValues[1]),
                workingDays = null,
                workingSchedule = EntityRef.create(
                    AppName.EMODEL,
                    WORKING_SCHEDULE_SOURCE_ID,
                    matchResult.groupValues[2]
                )
            )
        }

        businessDaysRegex.matches(this) -> {
            val matchResult = businessDaysRegex.find(this)!!
            TaskDueDateManual(
                durationType = DurationType.BUSINESS,
                duration = null,
                workingDays = matchResult.groupValues[1].toInt(),
                workingSchedule = EntityRef.create(
                    AppName.EMODEL,
                    WORKING_SCHEDULE_SOURCE_ID,
                    matchResult.groupValues[2]
                )
            )
        }

        else -> error("Invalid TaskDueDateManual expression: $this")
    }

    dueDateManual.validate("")

    return dueDateManual
}
