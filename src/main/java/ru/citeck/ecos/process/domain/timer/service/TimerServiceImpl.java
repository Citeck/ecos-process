package ru.citeck.ecos.process.domain.timer.service;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandsService;
import ru.citeck.ecos.commands.dto.CommandResult;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.process.domain.common.repo.EntityUuid;
import ru.citeck.ecos.process.domain.timer.entity.TimerEntity;
import ru.citeck.ecos.process.domain.timer.dto.TimerCommandDto;
import ru.citeck.ecos.process.domain.timer.dto.TimerDto;
import ru.citeck.ecos.process.domain.timer.repo.TimerRepository;
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService;
import ru.citeck.ecos.process.domain.timer.command.createtimer.CreateTimerCommand;
import ru.citeck.ecos.process.domain.timer.command.createtimer.CreateTimerCommandRes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class TimerServiceImpl implements TimerService {

    private final TimerRepository timerRepository;
    private final ProcTenantService tenantService;
    private final CommandsService commandsService;

    @Value("${ecos-process.timers.delay-after-fail-ms}")
    private Integer delayAfterFailMs;

    @Override
    public CreateTimerCommandRes createTimer(CreateTimerCommand createTimerCommand) {

        TimerEntity entity = timerCreationCommandToEntity(createTimerCommand);
        entity = timerRepository.save(entity);

        return new CreateTimerCommandRes(entity.getId().getId().toString());
    }

    private TimerEntity timerCreationCommandToEntity(CreateTimerCommand createTimerCommand) {

        UUID id = UUID.randomUUID();

        TimerEntity entity = new TimerEntity();

        entity.setId(new EntityUuid(tenantService.getCurrent(), id));
        entity.setTriggerTime(createTimerCommand.getTriggerTime());
        entity.setCommand(Json.getMapper().toString(createTimerCommand.getCommand()));

        return entity;
    }

    private Optional<TimerEntity> getFirstCompletedTimer() {
        return timerRepository.findFirstByActiveAndTriggerTimeBefore(true, Instant.now());
    }

    @Override
    public boolean cancelTimer(UUID timerId) {

        EntityUuid id = new EntityUuid(tenantService.getCurrent(), timerId);
        TimerEntity entity = timerRepository.findFirstByActiveAndId(true, id).orElse(null);

        if (entity != null) {
            entity.setActive(false);
            timerRepository.save(entity);
            return true;
        }
        return false;
    }

    @Override
    public void updateTimers() {

        Optional<TimerEntity> timerOpt;
        while ((timerOpt = getFirstCompletedTimer()).isPresent()) {

            TimerEntity timer = timerOpt.get();
            log.info("Timer trigger time was reached. Timer ID: {}, Time: {}",
                timer.getId(),
                timer.getTriggerTime()
            );

            CommandResult result = commandsService.executeSync(builder -> timerToCommand(builder, timer));
            if (isSuccessCommandRes(result)) {
                timer.setActive(false);
            } else {
                int retries = timer.getRetryCounter() + 1;
                timer.setRetryCounter(retries);
                timer.setTriggerTime(nextTriggerTimeAfterFail(retries));
                log.error("Failed to execute timer command. Timer ID: '{}'. Result: {}",
                    timer.getId(),
                    result.getErrors()
                );
            }
            timer.setResult(Json.getMapper().toString(result));
            timerRepository.save(timer);
        }
    }

    private Instant nextTriggerTimeAfterFail(int retries) {
        return Instant.now().plus(delayAfterFailMs * retries, ChronoUnit.MILLIS);
    }

    private boolean isSuccessCommandRes(CommandResult result) {
        return result.getErrors().size() == 0;
    }

    private Unit timerToCommand(CommandsService.CommandBuilder builder, TimerEntity entity) {

        TimerCommandDto timerCommand = Json.getMapper().read(entity.getCommand(), TimerCommandDto.class);

        if (timerCommand == null) {
            throw new IllegalStateException("Incorrect command: '" + entity.getCommand() + "'");
        }
        builder.setTenant(tenantService.getTenant(entity.getId().getTnt()));
        builder.setId(timerCommand.getId());
        builder.setTargetApp(timerCommand.getTargetApp());
        builder.setType(timerCommand.getType());

        ObjectData body = ObjectData.deepCopy(timerCommand.getBody());
        if (body != null && !body.has("timerId")) {
            body.set("timerId", entity.getId().getId());
        }
        builder.setBody(body);

        return Unit.INSTANCE;
    }

    private TimerDto entityToDto(TimerEntity entity) {

        TimerDto dto = new TimerDto();

        dto.setId(entity.getId().getId());
        dto.setRetryCounter(entity.getRetryCounter());
        dto.setTriggerTime(entity.getTriggerTime());
        dto.setActive(entity.isActive());
        dto.setCommand(Json.getMapper().read(entity.getCommand(), TimerCommandDto.class));
        dto.setResult(Json.getMapper().read(entity.getResult(), CommandResult.class));

        return dto;
    }

    @Override
    public void save(TimerDto dto) {
        timerRepository.save(dtoToEntity(dto));
    }

    private TimerEntity dtoToEntity(TimerDto dto) {

        TimerEntity entity = timerRepository.findById(new EntityUuid(tenantService.getCurrent(), dto.getId()))
            .orElseThrow(() -> new IllegalArgumentException("TimerEntity with id '" + dto.getId() + "' not found"));

        entity.setRetryCounter(dto.getRetryCounter());
        entity.setTriggerTime(dto.getTriggerTime());
        entity.setActive(dto.isActive());
        entity.setCommand(Json.getMapper().toString(dto.getCommand()));
        entity.setResult(Json.getMapper().toString(dto.getResult()));

        return entity;
    }
}
