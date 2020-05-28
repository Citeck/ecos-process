package ru.citeck.ecos.process.domain.proc.command.createproc;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessInstanceDto;
import ru.citeck.ecos.process.domain.proc.service.ProcService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateProcExecutor implements CommandExecutor<CreateProc> {

    private final ProcService procService;

    @Nullable
    @Override
    public CreateProcResp execute(CreateProc createProc) {

        UUID procDefRevId = UUID.fromString(createProc.getProcDefRevId());
        NewProcessInstanceDto instance = RemoteRecordsUtils.runAsSystem(() ->
            procService.createProcessInstance(createProc.getRecordRef(), procDefRevId)
        );

        return new CreateProcResp(
            instance.getId().toString(),
            instance.getStateId().toString(),
            instance.getStateData()
        );
    }
}
