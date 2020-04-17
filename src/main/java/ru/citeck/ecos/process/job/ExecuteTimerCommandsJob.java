package ru.citeck.ecos.process.job;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.service.TimerService;

@Component
@RequiredArgsConstructor
public class ExecuteTimerCommandsJob {

    private final TimerService timerService;

    @Scheduled(fixedRateString = "${ecos-process.timers.update-rate-ms}")
    public void execute() {
        timerService.updateTimers();
    }
}
