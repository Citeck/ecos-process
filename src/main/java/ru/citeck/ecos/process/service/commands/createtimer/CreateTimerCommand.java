package ru.citeck.ecos.process.service.commands.createtimer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.citeck.ecos.commands.annotation.CommandType;
import ru.citeck.ecos.process.dto.TimerCommandDto;

import java.time.Instant;

@Data
@CommandType("create-timer")
@AllArgsConstructor
@NoArgsConstructor
public class CreateTimerCommand {
    private Instant triggerTime;
    private TimerCommandDto command;
}
