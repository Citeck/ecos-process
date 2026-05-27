package ru.citeck.ecos.process.domain.proctask.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_COMMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_LA_COMPLETE_KEY
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import java.time.Instant

class CompleteTaskDataTest {

    private fun completeTaskData(variables: Map<String, Any?>): CompleteTaskData {
        return CompleteTaskData(
            task = ProcTaskDto(id = "task-1", created = Instant.now()),
            outcome = Outcome.EMPTY,
            variables = variables
        )
    }

    @Test
    fun `getLaCompleted returns true when la flag is true`() {
        val data = completeTaskData(mapOf(BPMN_LA_COMPLETE_KEY to true))

        assertThat(data.getLaCompleted()).isTrue()
    }

    @Test
    fun `getLaCompleted returns false when la flag is false`() {
        val data = completeTaskData(mapOf(BPMN_LA_COMPLETE_KEY to false))

        assertThat(data.getLaCompleted()).isFalse()
    }

    @Test
    fun `getLaCompleted returns false when la flag is not Boolean or absent`() {
        assertThat(completeTaskData(emptyMap()).getLaCompleted()).isFalse()
        assertThat(completeTaskData(mapOf(BPMN_LA_COMPLETE_KEY to "true")).getLaCompleted()).isFalse()
    }

    @Test
    fun `getComment returns value when present`() {
        val data = completeTaskData(mapOf(BPMN_COMMENT to "hello"))

        assertThat(data.getComment()).isEqualTo("hello")
    }

    @Test
    fun `getComment returns null when value is blank or empty`() {
        assertThat(completeTaskData(mapOf(BPMN_COMMENT to "   ")).getComment()).isNull()
        assertThat(completeTaskData(mapOf(BPMN_COMMENT to "")).getComment()).isNull()
    }

    @Test
    fun `getComment returns null when value is missing or not a String`() {
        assertThat(completeTaskData(emptyMap()).getComment()).isNull()
        assertThat(completeTaskData(mapOf(BPMN_COMMENT to 42)).getComment()).isNull()
    }
}
