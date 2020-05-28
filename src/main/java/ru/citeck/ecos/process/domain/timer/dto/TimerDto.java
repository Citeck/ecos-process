package ru.citeck.ecos.process.domain.timer.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.citeck.ecos.commands.dto.CommandResult;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@ToString(exclude = {"command", "result"})
@EqualsAndHashCode
public class TimerDto {
    private UUID id;
    private int retryCounter;
    private Instant triggerTime;
    private boolean active;
    private TimerCommandDto command;
    private CommandResult result;
}
