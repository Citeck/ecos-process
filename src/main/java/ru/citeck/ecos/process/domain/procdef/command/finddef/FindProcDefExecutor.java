package ru.citeck.ecos.process.domain.procdef.command.finddef;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

@Component
@RequiredArgsConstructor
public class FindProcDefExecutor implements CommandExecutor<FindProcDef> {

    private final ProcDefService procDefService;

    @Nullable
    @Override
    public FindProcDefResp execute(FindProcDef findProcDef) {

        ProcDefRevDto procDefRev = RemoteRecordsUtils.runAsSystem(() ->
            procDefService.findProcDef(
                findProcDef.getProcType(),
                findProcDef.getEcosTypeRef(),
                findProcDef.getAlfTypes()
            )
        );

        if (procDefRev == null) {
            return null;
        }

        return new FindProcDefResp(
            procDefRev.getProcDefId(),
            procDefRev.getId().toString()
        );
    }
}
