package ru.citeck.ecos.process.domain.cmmn.eapps;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler;
import ru.citeck.ecos.process.domain.cmmn.io.CmmnProcDefImporter;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;

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
    }
}
