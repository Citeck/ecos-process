package ru.citeck.ecos.process.domain.procdef.command.getprocdefrev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.context.lib.auth.AuthContext;
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO;
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records3.RecordsServiceFactory;
import ru.citeck.ecos.records3.record.request.RequestContext;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class GetProcDefRevExecutor implements CommandExecutor<GetProcDefRev> {

    private final RecordsServiceFactory recordsServices;
    private final ProcDefService procDefService;
    private final ProcDefRevDataProvider procDefRevDataProvider;
    private final CmmnIO cmmnIO;

    @Nullable
    @Override
    public GetProcDefRevResp execute(GetProcDefRev findProcDef) {

        UUID revId = UUID.fromString(findProcDef.getProcDefRevId());
        ProcDefRevDto procDefRev = AuthContext.runAsSystem(() ->
            procDefService.getProcessDefRev(findProcDef.getProcType(), revId)
        );

        if (procDefRev == null) {
            return null;
        }

        GetProcDefRevResp resp = new GetProcDefRevResp();

        byte[] data;
        if ("ecos-cmmn".equals(procDefRev.getFormat())) {
            CmmnProcessDef def = Json.getMapper().read(procDefRev.loadData(procDefRevDataProvider), CmmnProcessDef.class);
            if (def == null) {
                throw new RuntimeException("Proc def can't be readed: "
                    + procDefRev.getProcDefId() + " "
                    + procDefRev.getId());
            }
            data = RequestContext.doWithCtx(recordsServices, ctx -> {
                String cmmnDef = cmmnIO.exportAlfCmmnToString(def);
                log.info(cmmnDef);
                return cmmnDef.getBytes(StandardCharsets.UTF_8);
            });
        } else {
            data = procDefRev.loadData(procDefRevDataProvider);
        }

        resp.setData(data);
        resp.setFormat(procDefRev.getFormat());
        resp.setId(procDefRev.getId().toString());
        resp.setProcDefId(procDefRev.getProcDefId());
        resp.setVersion(procDefRev.getVersion());

        return resp;
    }
}
