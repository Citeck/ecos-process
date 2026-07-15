package ru.citeck.ecos.process.domain.timer.job;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.config.mongo.MongoDisabledEnvironmentPostProcessor;
import ru.citeck.ecos.process.domain.timer.service.TimerService;

@Component
@RequiredArgsConstructor
@Profile("!test")
@ConditionalOnProperty(value = MongoDisabledEnvironmentPostProcessor.MONGO_ENABLED_PROP, havingValue = "true", matchIfMissing = true)
public class ExecuteTimerCommandsJob {

    private final TimerService timerService;

    @Scheduled(fixedRateString = "${ecos-process.timers.update-rate-ms}")
    public void execute() {
        timerService.updateTimers();
    }
}
