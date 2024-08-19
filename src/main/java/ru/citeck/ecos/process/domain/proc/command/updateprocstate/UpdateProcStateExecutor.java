package ru.citeck.ecos.process.domain.proc.command.updateprocstate;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.process.domain.proc.dto.ProcessStateDto;
import ru.citeck.ecos.process.domain.proc.service.ProcService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UpdateProcStateExecutor implements CommandExecutor<UpdateProcState> {

    private final ProcService procService;

    @Nullable
    @Override
    public UpdateProcStateResp execute(UpdateProcState updateProcState) {

        UUID prevProcStateId = UUID.fromString(updateProcState.getPrevProcStateId());
        ProcessStateDto newState = AuthContext.runAsSystem(() ->
            procService.updateStateData(prevProcStateId, updateProcState.getStateData())
        );

        return new UpdateProcStateResp(newState.getId().toString(), newState.getVersion());
    }
}
