package ru.citeck.ecos.process.domain.procdef.command.getprocdefrev;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.commands.CommandExecutor;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO;
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcDef;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.rest.RemoteRecordsUtils;
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

        byte[] data;
        if ("ecos-cmmn".equals(procDefRev.getFormat())) {
            CmmnProcDef def = Json.getMapper().read(procDefRev.getData(), CmmnProcDef.class);
            if (def == null) {
                throw new RuntimeException("Proc def can't be readed: "
                    + procDefRev.getProcDefId() + " "
                    + procDefRev.getId());
            }
            data = RequestContext.doWithCtx(recordsServices, ctx -> {
                String cmmnDef = CmmnIO.exportAlfCmmnToString(def);
                log.info(cmmnDef);
                return cmmnDef.getBytes(StandardCharsets.UTF_8);
            });
        } else {
            data = procDefRev.getData();
        }

        resp.setData(data);
        resp.setFormat(procDefRev.getFormat());
        resp.setId(procDefRev.getId().toString());
        resp.setProcDefId(procDefRev.getProcDefId());
        resp.setVersion(procDefRev.getVersion());

        return resp;
    }
}
