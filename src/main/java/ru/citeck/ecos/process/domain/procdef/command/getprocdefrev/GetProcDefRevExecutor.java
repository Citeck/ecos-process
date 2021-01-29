package ru.citeck.ecos.process.domain.procdef.command.getprocdefrev;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetProcDefRevExecutor implements CommandExecutor<GetProcDefRev> {

    private final ProcDefService procDefService;

    @Nullable
    @Override
    public GetProcDefRevResp execute(GetProcDefRev findProcDef) {

        UUID revId = UUID.fromString(findProcDef.getProcDefRevId());
        ProcDefRevDto procDefRev = RemoteRecordsUtils.runAsSystem(() ->
            procDefService.getProcessDefRev(findProcDef.getProcType(), revId)
        );

        if (procDefRev == null) {
            return null;
        }

        GetProcDefRevResp resp = new GetProcDefRevResp();
        resp.setData(procDefRev.getData());
        resp.setFormat(procDefRev.getFormat());
        resp.setId(procDefRev.getId().toString());
        resp.setProcDefId(procDefRev.getProcDefId());
        resp.setVersion(procDefRev.getVersion());

        return resp;
    }
}
