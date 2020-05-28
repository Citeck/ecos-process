package ru.citeck.ecos.process.domain.procdef.command.finddef;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FindProcDefExecutor implements CommandExecutor<FindProcDef> {

    private final ProcDefService procDefService;

    @Nullable
    @Override
    public FindProcDefResp execute(FindProcDef findProcDef) {

        Optional<ProcDefRevDto> optProcDef = RemoteRecordsUtils.runAsSystem(() ->
            procDefService.findProcDef(
                findProcDef.getProcType(),
                findProcDef.getEcosTypeRef(),
                findProcDef.getAlfTypes()
            )
        );

        if (!optProcDef.isPresent()) {
            return null;
        }

        ProcDefRevDto procDefRev = optProcDef.get();

        return new FindProcDefResp(
            procDefRev.getProcDefId(),
            procDefRev.getId().toString()
        );
    }
}
