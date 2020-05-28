package ru.citeck.ecos.process.domain.timer.service;

import ru.citeck.ecos.process.domain.timer.dto.TimerDto;
import ru.citeck.ecos.process.domain.timer.command.createtimer.CreateTimerCommand;
import ru.citeck.ecos.process.domain.timer.command.createtimer.CreateTimerCommandRes;

import java.util.UUID;

public interface TimerService {

    CreateTimerCommandRes createTimer(CreateTimerCommand timer);

    void updateTimers();

    boolean cancelTimer(UUID timerId);

    void save(TimerDto dto);
}
