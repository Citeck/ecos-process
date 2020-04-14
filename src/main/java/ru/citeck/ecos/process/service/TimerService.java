package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.TimerDto;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommand;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommandRes;

import java.util.Optional;

public interface TimerService {
    CreateTimerCommandRes createTimer(CreateTimerCommand timer);

    Optional<TimerDto> findFirstActiveTimerByTenant(Integer tenant);

    void save(TimerDto dto);
}
