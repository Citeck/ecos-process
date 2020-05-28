package ru.citeck.ecos.process.domain.procdef.eapp.casetemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseTemplateModuleHandler implements EcosModuleHandler<CaseTemplateDto> {

    private static final String CASE_TEMPLATE_TYPE = "process/cmmn";

    private final ProcDefService processService;

    @Override
    public void deployModule(@NotNull CaseTemplateDto dto) {
        log.info("Case template module received: " + dto.getFilePath());
        processService.uploadProcDef(CaseTemplateUtils.parseCmmn(dto.getFilePath(), dto.getData()));
    }

    @NotNull
    @Override
    public ModuleWithMeta<CaseTemplateDto> getModuleMeta(@NotNull CaseTemplateDto dto) {
        NewProcessDefDto procDefDto = CaseTemplateUtils.parseCmmn(dto.getFilePath(), dto.getData());
        return new ModuleWithMeta<>(dto, new ModuleMeta(procDefDto.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return CASE_TEMPLATE_TYPE;
    }

    @Override
    public void listenChanges(@NotNull Consumer<CaseTemplateDto> consumer) {

    }

    @Nullable
    @Override
    public ModuleWithMeta<CaseTemplateDto> prepareToDeploy(@NotNull CaseTemplateDto caseTemplateDTO) {
        return getModuleMeta(caseTemplateDTO);
    }
}
