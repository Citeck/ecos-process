package ru.citeck.ecos.process.job;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandsService;
import ru.citeck.ecos.commands.dto.CommandResult;
import ru.citeck.ecos.process.dto.TimerCommandDto;
import ru.citeck.ecos.process.dto.TimerDto;
import ru.citeck.ecos.process.service.TimerService;
import ru.citeck.ecos.process.service.impl.ProcessTenantService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExecuteTimerCommandsJob {
    private final TimerService timerService;
    private final CommandsService commandsService;
    private final ProcessTenantService tenantService;

    @Value("${jobs.execute-timer-commands-job.delay-after-fail-millis}")
    private Integer delayAfterFail;

    @Scheduled(fixedRateString = "${jobs.execute-timer-commands-job.rate-millis}")
    public void execute() {
        Integer tenantId = tenantService.getCurrent();

        Optional<TimerDto> dtoOpt;
        while ((dtoOpt = timerService.findFirstActiveTimerByTenant(tenantId)).isPresent()) {
            TimerDto dto = dtoOpt.get();

            CommandResult result = commandsService.executeSync(builder -> timerCommandToCommandBuilder(dto, builder));
            if (isSuccess(result)) {
                dto.setActive(false);
            } else {
                postponeExecution(dto);
                log.error("Failed to execute timer command. Timer id: '{}'", dto.getId());
            }
            dto.setResult(result);
            timerService.save(dto);
        }
    }

    private Unit timerCommandToCommandBuilder(TimerDto dto, CommandsService.CommandBuilder builder) {
        TimerCommandDto timerCommand = dto.getCommand();

        builder.setId(timerCommand.getId());
        builder.setTargetApp(timerCommand.getTargetApp());
        builder.setType(timerCommand.getType());
        builder.setBody(timerCommand.getBody());

        return Unit.INSTANCE;
    }

    private boolean isSuccess(CommandResult result) {
        return result.getErrors().isEmpty();
    }

    private void postponeExecution(TimerDto dto) {
        int retries = dto.getRetryCounter();
        retries += 1;
        dto.setRetryCounter(retries);
        dto.setTriggerTime(nextTriggerTimeAfterFail(retries));
    }

    private Instant nextTriggerTimeAfterFail(int retries) {
        return Instant.now().plus(delayAfterFail * retries, ChronoUnit.MILLIS);
    }
}
