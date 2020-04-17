package ru.citeck.ecos.process.service.commands.createtimer;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimerCommandRes {
    private String timerId;
}
