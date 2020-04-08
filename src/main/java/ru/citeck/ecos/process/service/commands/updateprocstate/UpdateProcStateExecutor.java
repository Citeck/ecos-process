package ru.citeck.ecos.process.service.commands.updateprocstate;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.dto.ProcessStateDto;
import ru.citeck.ecos.process.service.ProcessService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateProcStateExecutor implements CommandExecutor<UpdateProcState> {

    private final ProcessService processService;

    @Nullable
    @Override
    public UpdateProcStateResp execute(UpdateProcState updateProcState) {

        UUID prevProcStateId = UUID.fromString(updateProcState.getPrevProcStateId());
        ProcessStateDto newState = processService.updateStateData(prevProcStateId, updateProcState.getStateData());

        return new UpdateProcStateResp(newState.getId().toString(), newState.getVersion());
    }
}
