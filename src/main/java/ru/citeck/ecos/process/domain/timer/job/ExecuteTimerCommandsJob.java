package ru.citeck.ecos.process.domain.timer.job;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.timer.service.TimerService;

@Component
@RequiredArgsConstructor
@Profile("!test")
public class ExecuteTimerCommandsJob {

    private final TimerService timerService;

    @Scheduled(fixedRateString = "${ecos-process.timers.update-rate-ms}")
    public void execute() {
        timerService.updateTimers();
    }
}
