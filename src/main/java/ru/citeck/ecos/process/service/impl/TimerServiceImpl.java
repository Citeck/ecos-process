package ru.citeck.ecos.process.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.dto.CommandResult;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.TimerCommandEntity;
import ru.citeck.ecos.process.domain.TimerEntity;
import ru.citeck.ecos.process.dto.TimerCommandDto;
import ru.citeck.ecos.process.dto.TimerDto;
import ru.citeck.ecos.process.repository.TimerRepository;
import ru.citeck.ecos.process.service.TimerService;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommand;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommandRes;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TimerServiceImpl implements TimerService {
    private final TimerRepository timerRepository;
    private final ProcessTenantService tenantService;

    @Override
    public CreateTimerCommandRes createTimer(CreateTimerCommand createTimerCommand) {

        TimerEntity entity = timerCreationCommandToEntity(createTimerCommand);

        timerRepository.save(entity);

        return new CreateTimerCommandRes(entity.getId().getId().toString());
    }

    private TimerEntity timerCreationCommandToEntity(CreateTimerCommand createTimerCommand) {
        UUID id = UUID.randomUUID();

        TimerEntity entity = new TimerEntity();

        entity.setId(new EntityUuid(tenantService.getCurrent(), id));
        entity.setTriggerTime(createTimerCommand.getTriggerTime());
        entity.setCommand(timerCommandDtoToEntity(createTimerCommand.getCommand()));

        return entity;
    }

    @Override
    public Optional<TimerDto> findFirstActiveTimerByTenant(Integer tenant) {
        return timerRepository.findFirstByIdTntAndActiveAndTriggerTimeBefore(tenant, true, Instant.now())
            .map(this::entityToDto);
    }

    private TimerDto entityToDto(TimerEntity entity) {
        TimerDto dto = new TimerDto();

        dto.setId(entity.getId().getId());
        dto.setRetryCounter(entity.getRetryCounter());
        dto.setTriggerTime(entity.getTriggerTime());
        dto.setActive(entity.getActive());
        dto.setCommand(timerCommandEntityToDto(entity.getCommand()));
        dto.setResult(Json.getMapper().read(entity.getResult(), CommandResult.class));

        return dto;
    }

    private TimerCommandDto timerCommandEntityToDto(TimerCommandEntity entity) {
        TimerCommandDto dto = new TimerCommandDto();

        dto.setId(entity.getId());
        dto.setTargetApp(entity.getTargetApp());
        dto.setType(entity.getType());
        dto.setBody(Json.getMapper().read(entity.getBody()));

        return dto;
    }

    @Override
    public void save(TimerDto dto) {
        timerRepository.save(dtoToEntity(dto));
    }

    private TimerEntity dtoToEntity(TimerDto dto) {
        TimerEntity entity = timerRepository.findById_Id(dto.getId())
            .orElseThrow(() -> new IllegalArgumentException("TimerEntity with id '" + dto.getId() + "' not found"));

        entity.setRetryCounter(dto.getRetryCounter());
        entity.setTriggerTime(dto.getTriggerTime());
        entity.setActive(dto.isActive());
        entity.setCommand(timerCommandDtoToEntity(dto.getCommand()));
        entity.setResult(Json.getMapper().toString(dto.getResult()));

        return entity;
    }

    private TimerCommandEntity timerCommandDtoToEntity(TimerCommandDto dto) {
        TimerCommandEntity entity = new TimerCommandEntity();

        entity.setId(dto.getId());
        entity.setTargetApp(dto.getTargetApp());
        entity.setType(dto.getType());
        entity.setBody(Json.getMapper().toString(dto.getBody()));

        return entity;
    }
}
