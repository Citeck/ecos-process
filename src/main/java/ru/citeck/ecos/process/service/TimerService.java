package ru.citeck.ecos.process.service;

import ru.citeck.ecos.process.dto.TimerDto;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommand;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommandRes;

import java.util.UUID;

public interface TimerService {

    CreateTimerCommandRes createTimer(CreateTimerCommand timer);

    void updateTimers();

    boolean cancelTimer(UUID timerId);

    void save(TimerDto dto);
}
