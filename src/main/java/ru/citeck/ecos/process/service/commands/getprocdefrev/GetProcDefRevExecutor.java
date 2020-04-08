package ru.citeck.ecos.process.service.commands.getprocdefrev;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.dto.ProcessDefRevDto;
import ru.citeck.ecos.process.service.ProcessDefService;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetProcDefRevExecutor implements CommandExecutor<GetProcDefRev> {

    private final ProcessDefService processDefService;

    @Nullable
    @Override
    public GetProcDefRevResp execute(GetProcDefRev findProcDef) {

        UUID revId = UUID.fromString(findProcDef.getProcDefRevId());
        Optional<ProcessDefRevDto> optDefRev = processDefService.getProcessDefRev(findProcDef.getProcType(), revId);

        if (!optDefRev.isPresent()) {
            return null;
        }

        ProcessDefRevDto procDefRev = optDefRev.get();

        GetProcDefRevResp resp = new GetProcDefRevResp();
        resp.setData(procDefRev.getData());
        resp.setFormat(procDefRev.getFormat());
        resp.setId(procDefRev.getId().toString());
        resp.setProcDefId(procDefRev.getProcDefId());
        resp.setVersion(procDefRev.getVersion());

        return resp;
    }
}
