package ru.citeck.ecos.process.domain.bpmn

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.DurationType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.TaskDueDateManual
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.WORKING_SCHEDULE_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.toExpression
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.user.toTaskDueDateManual
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Duration

class TaskDueDateManualExpressionConverterTest {

    companion object {
        private val scheduleRef = EntityRef.create(AppName.EMODEL, WORKING_SCHEDULE_SOURCE_ID, "some-schedule")
    }

    @Test
    fun `convert calendar duration to expression`() {
        val taskDueDateManual = TaskDueDateManual(
            durationType = DurationType.CALENDAR,
            duration = Duration.ofHours(5),
            workingDays = null,
            workingSchedule = EntityRef.EMPTY
        )
        val expression = taskDueDateManual.toExpression()

        assertEquals("\${Time.nowPlus(\"PT5H\").toIsoString()}", expression)
    }

    @Test
    fun `convert calendar duration complex to expression`() {
        val taskDueDateManual = TaskDueDateManual(
            durationType = DurationType.CALENDAR,
            duration = Duration.ofHours(2).plusMinutes(30),
            workingDays = null,
            workingSchedule = EntityRef.EMPTY
        )
        val expression = taskDueDateManual.toExpression()

        assertEquals("\${Time.nowPlus(\"PT2H30M\").toIsoString()}", expression)
    }

    @Test
    fun `convert business days to expression`() {
        val taskDueDateManual = TaskDueDateManual(
            durationType = DurationType.BUSINESS,
            duration = Duration.ofDays(2),
            workingDays = null,
            workingSchedule = scheduleRef
        )
        val expression = taskDueDateManual.toExpression()

        assertEquals("\${Time.nowPlusWorkingTime(\"PT48H\", \"some-schedule\").toIsoString()}", expression)
    }

    @Test
    fun `convert business duration to expression`() {
        val taskDueDateManual = TaskDueDateManual(
            durationType = DurationType.BUSINESS,
            duration = null,
            workingDays = 10,
            workingSchedule = scheduleRef
        )
        val expression = taskDueDateManual.toExpression()

        assertEquals("\${Time.nowPlusWorkingDays(10, \"some-schedule\").toIsoString()}", expression)
    }

    @Test
    fun `convert expression to calendar duration`() {
        val expression = "\${Time.nowPlus(\"PT5H\").toIsoString()}"
        val taskDueDateManual = expression.toTaskDueDateManual()

        val expected = TaskDueDateManual(
            durationType = DurationType.CALENDAR,
            duration = Duration.ofHours(5),
            workingDays = null,
            workingSchedule = EntityRef.EMPTY
        )

        assertEquals(expected, taskDueDateManual)
    }

    @Test
    fun `convert expression to calendar duration complex`() {
        val expression = "\${Time.nowPlus(\"PT2H30M\").toIsoString()}"
        val taskDueDateManual = expression.toTaskDueDateManual()

        val expected = TaskDueDateManual(
            durationType = DurationType.CALENDAR,
            duration = Duration.ofHours(2).plusMinutes(30),
            workingDays = null,
            workingSchedule = EntityRef.EMPTY
        )

        assertEquals(expected, taskDueDateManual)
    }

    @Test
    fun `convert expression to business duration`() {
        val expression = "\${Time.nowPlusWorkingTime(\"PT48H\", \"some-schedule\").toIsoString()}"

        val taskDueDateManual = expression.toTaskDueDateManual()
        val expected = TaskDueDateManual(
            durationType = DurationType.BUSINESS,
            duration = Duration.ofDays(2),
            workingDays = null,
            workingSchedule = scheduleRef
        )

        assertEquals(expected, taskDueDateManual)
    }

    @Test
    fun `convert expression to business days`() {
        val expression = "\${Time.nowPlusWorkingDays(10, \"some-schedule\").toIsoString()}"

        val taskDueDateManual = expression.toTaskDueDateManual()
        val expected = TaskDueDateManual(
            durationType = DurationType.BUSINESS,
            duration = null,
            workingDays = 10,
            workingSchedule = scheduleRef
        )

        assertEquals(expected, taskDueDateManual)
    }

    @Test
    fun `convert calendar duration with minutes to expression`() {
        val taskDueDateManual = TaskDueDateManual(
            durationType = DurationType.CALENDAR,
            duration = Duration.ofMinutes(90),
            workingDays = null,
            workingSchedule = EntityRef.EMPTY
        )
        val expression = taskDueDateManual.toExpression()

        assertEquals("\${Time.nowPlus(\"PT1H30M\").toIsoString()}", expression)
    }

    @Test
    fun `convert business duration with hours to expression`() {
        val taskDueDateManual = TaskDueDateManual(
            durationType = DurationType.BUSINESS,
            duration = Duration.ofHours(8),
            workingDays = null,
            workingSchedule = scheduleRef
        )
        val expression = taskDueDateManual.toExpression()

        assertEquals("\${Time.nowPlusWorkingTime(\"PT8H\", \"some-schedule\").toIsoString()}", expression)
    }

    @Test
    fun `convert expression with hours to business duration`() {
        val expression = "\${Time.nowPlusWorkingTime(\"PT8H\", \"some-schedule\").toIsoString()}"
        val taskDueDateManual = expression.toTaskDueDateManual()

        val expected = TaskDueDateManual(
            durationType = DurationType.BUSINESS,
            duration = Duration.ofHours(8),
            workingDays = null,
            workingSchedule = scheduleRef
        )

        assertEquals(expected, taskDueDateManual)
    }

    @Test
    fun `convert expression with minutes to calendar duration`() {
        val expression = "\${Time.nowPlus(\"PT1H30M\").toIsoString()}"
        val taskDueDateManual = expression.toTaskDueDateManual()

        val expected = TaskDueDateManual(
            durationType = DurationType.CALENDAR,
            duration = Duration.ofMinutes(90),
            workingDays = null,
            workingSchedule = EntityRef.EMPTY
        )

        assertEquals(expected, taskDueDateManual)
    }

    @Test
    fun `convert with zero duration should throw`() {
        val expression = "\${Time.nowPlus(\"PT0S\").toIsoString()}"

        assertThrows<EcosBpmnElementDefinitionException> {
            expression.toTaskDueDateManual()
        }
    }

    @Test
    fun `convert with negative duration should throw`() {
        val expression = "\${Time.nowPlus(\"PT-1S\").toIsoString()}"

        assertThrows<EcosBpmnElementDefinitionException> {
            expression.toTaskDueDateManual()
        }
    }

    @Test
    fun `convert with zero working days should throw`() {
        val expression = "\${Time.nowPlusWorkingDays(0, \"some-schedule\").toIsoString()}"

        assertThrows<EcosBpmnElementDefinitionException> {
            expression.toTaskDueDateManual()
        }
    }

    @Test
    fun `convert with negative working days should throw`() {
        val expression = "\${Time.nowPlusWorkingDays(-1, \"some-schedule\").toIsoString()}"

        assertThrows<IllegalStateException> {
            expression.toTaskDueDateManual()
        }
    }

    @Test
    fun `create due date manual dto with both duration and working days should throw`() {
        assertThrows<EcosBpmnElementDefinitionException> {
            TaskDueDateManual(
                durationType = DurationType.BUSINESS,
                duration = Duration.ofDays(2),
                workingDays = 10,
                workingSchedule = scheduleRef
            ).validate("")
        }
    }

    @Test
    fun `create due date manual dto with calendar time and missing duration should throw`() {
        assertThrows<EcosBpmnElementDefinitionException> {
            TaskDueDateManual(
                durationType = DurationType.CALENDAR,
                duration = null,
                workingDays = null,
                workingSchedule = EntityRef.EMPTY
            ).validate("")
        }
    }
}
