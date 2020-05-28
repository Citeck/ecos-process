package ru.citeck.ecos.process.domain.timer.command.createtimer;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.domain.timer.service.TimerService;

@Component
@RequiredArgsConstructor
public class CreateTimerCommandExecutor implements CommandExecutor<CreateTimerCommand> {

    private final TimerService timerService;

    @Nullable
    @Override
    public CreateTimerCommandRes execute(CreateTimerCommand createTimerCommand) {
        return timerService.createTimer(createTimerCommand);
    }
}
