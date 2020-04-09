package ru.citeck.ecos.process.service.commands.finddef;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.process.dto.ProcessDefRevDto;
import ru.citeck.ecos.process.service.ProcessDefService;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class FindProcDefExecutor implements CommandExecutor<FindProcDef> {

    private final ProcessDefService processDefService;

    @Nullable
    @Override
    public FindProcDefResp execute(FindProcDef findProcDef) {

        Optional<ProcessDefRevDto> optProcDef = processDefService.findProcDef(
            findProcDef.getProcType(),
            findProcDef.getEcosTypeRef(),
            findProcDef.getAlfTypes()
        );

        if (!optProcDef.isPresent()) {
            return null;
        }

        ProcessDefRevDto procDefRev = optProcDef.get();

        return new FindProcDefResp(
            procDefRev.getProcDefId(),
            procDefRev.getId().toString()
        );
    }
}
