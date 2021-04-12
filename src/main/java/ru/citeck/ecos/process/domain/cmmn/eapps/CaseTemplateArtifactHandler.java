package ru.citeck.ecos.process.domain.cmmn.eapps;

import kotlin.Unit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.process.domain.cmmn.io.CmmnFormat;
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO;
import ru.citeck.ecos.process.domain.cmmn.io.CmmnProcDefImporter;
import ru.citeck.ecos.process.domain.cmmn.model.ecos.CmmnProcessDef;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseTemplateArtifactHandler implements EcosArtifactHandler<CaseTemplateDto> {

    private static final String CASE_TEMPLATE_TYPE = "process/cmmn";

    private final ProcDefService processService;
    private final CmmnProcDefImporter cmmnProcDefImporter;

    @Override
    public void deployArtifact(@NotNull CaseTemplateDto dto) {
        log.info("Case template module received: " + dto.getFilePath());
        String fileName = dto.getFilePath();
        int slashIdx = fileName.indexOf('/');
        if (slashIdx > -1) {
            fileName = fileName.substring(slashIdx + 1);
        }
        processService.uploadProcDef(cmmnProcDefImporter.getDataToImport(dto.getData(), fileName));
    }

    @NotNull
    @Override
    public String getArtifactType() {
        return CASE_TEMPLATE_TYPE;
    }

    @Override
    public void listenChanges(@NotNull Consumer<CaseTemplateDto> consumer) {
        processService.listenChanges("cmmn", procDefDto -> {

            CaseTemplateDto caseTemplateDto = new CaseTemplateDto();
            caseTemplateDto.setFilePath(procDefDto.getId());
            if (!caseTemplateDto.getFilePath().endsWith(".xml")) {
                caseTemplateDto.setFilePath(caseTemplateDto.getFilePath() + ".xml");
            }

            ProcDefRevDto rev = processService.getProcessDefRev("cmmn", procDefDto.getRevisionId());

            if (rev == null) {
                throw new RuntimeException("Revision doesn't found for procDef: " + procDefDto.getId());
            }

            byte[] data;
            if (CmmnFormat.LEGACY_CMMN.getCode().equals(procDefDto.getFormat())) {

                data = rev.getData();

            } else if (CmmnFormat.ECOS_CMMN.getCode().equals(procDefDto.getFormat())) {

                CmmnProcessDef procDef = Json.getMapper().read(rev.getData(), CmmnProcessDef.class);
                if (procDef == null) {
                    throw new RuntimeException("CMMN process reading failed: " + procDefDto.getId());
                }
                data = CmmnIO.exportEcosCmmnToString(procDef).getBytes(StandardCharsets.UTF_8);
            } else {

                throw new RuntimeException("Unknown format: " + procDefDto.getFormat());
            }

            caseTemplateDto.setData(data);
            consumer.accept(caseTemplateDto);

            return Unit.INSTANCE;
        });
    }
}
