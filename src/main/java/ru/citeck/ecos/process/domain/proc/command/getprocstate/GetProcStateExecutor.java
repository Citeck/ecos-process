package ru.citeck.ecos.process.domain.proc.command.getprocstate;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.domain.proc.dto.ProcessStateDto;
import ru.citeck.ecos.process.domain.proc.service.ProcService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetProcStateExecutor implements CommandExecutor<GetProcState> {

    private final ProcService procService;

    @Nullable
    @Override
    public GetProcStateResp execute(GetProcState getProcState) {

        UUID procStateId = UUID.fromString(getProcState.getProcStateId());
        ProcessStateDto procState = RemoteRecordsUtils.runAsSystem(() ->
            procService.getProcStateByStateId(getProcState.getProcType(), procStateId)
        );

        if (procState == null) {
            return null;
        }

        return new GetProcStateResp(
            procState.getProcDefRevId().toString(),
            procState.getData(),
            procState.getVersion()
        );
    }
}
