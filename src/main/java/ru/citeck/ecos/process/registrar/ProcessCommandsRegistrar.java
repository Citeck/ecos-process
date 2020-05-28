package ru.citeck.ecos.process.registrar;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.commands.CommandsService;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ProcessCommandsRegistrar {

    private final List<CommandExecutor<?>> executors;
    private final CommandsService commandsService;

    @PostConstruct
    void init() {
        executors.forEach(commandsService::addExecutor);
    }
}
