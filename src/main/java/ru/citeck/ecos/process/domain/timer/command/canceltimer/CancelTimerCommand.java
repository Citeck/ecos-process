package ru.citeck.ecos.process.domain.timer.command.canceltimer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.commands.annotation.CommandType;
import ru.citeck.ecos.process.domain.timer.service.TimerService;

import java.util.UUID;

@Component
public class CancelTimerCommand {

    @Component
    @RequiredArgsConstructor
    public static class Executor implements CommandExecutor<Command> {

        private final TimerService timerService;

        @Nullable
        @Override
        public Object execute(Command command) {
            return new Result(timerService.cancelTimer(UUID.fromString(command.timerId)));
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Result {
        private boolean wasCancelled;
    }

    @Data
    @CommandType("cancel-timer")
    public static class Command {
        private String timerId;
    }
}
