package ru.citeck.ecos.process.domain.timer

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.Mockito
import org.mockito.kotlin.verifyNoInteractions
import ru.citeck.ecos.commands.CommandsService
import ru.citeck.ecos.process.domain.timer.command.createtimer.CreateTimerCommand
import ru.citeck.ecos.process.domain.timer.dto.TimerDto
import ru.citeck.ecos.process.domain.timer.service.TimerServiceImpl
import java.time.Instant
import java.util.UUID
import kotlin.test.Test

/**
 * When MongoDB is disabled the timer repository bean is absent. The timer subsystem must then
 * fail fast with an actionable error instead of an NPE, and the periodic update must be a no-op
 * (the job itself is also not registered, see ExecuteTimerCommandsJob conditional).
 */
class TimerServiceMongoDisabledTest {

    private val commandsService = Mockito.mock(CommandsService::class.java)
    private val timerService = TimerServiceImpl(commandsService)

    @Test
    fun `createTimer without repository fails with actionable error`() {
        assertThatThrownBy { timerService.createTimer(CreateTimerCommand(Instant.now(), null)) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("MongoDB is disabled")
    }

    @Test
    fun `cancelTimer without repository fails with actionable error`() {
        assertThatThrownBy { timerService.cancelTimer(UUID.randomUUID()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("MongoDB is disabled")
    }

    @Test
    fun `save without repository fails with actionable error`() {
        assertThatThrownBy { timerService.save(TimerDto()) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("MongoDB is disabled")
    }

    @Test
    fun `updateTimers without repository is a no-op`() {
        timerService.updateTimers()

        verifyNoInteractions(commandsService)
    }

    @Test
    fun `init without repository does not fail`() {
        // only logs the startup warning about the inactive timer subsystem
        timerService.init()
    }
}
