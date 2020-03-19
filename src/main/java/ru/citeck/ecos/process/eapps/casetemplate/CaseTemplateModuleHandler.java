package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.process.service.mapper.CaseTemplateMapper;
import ru.citeck.ecos.process.service.CaseTemplateService;
import ru.citeck.ecos.process.service.dto.CaseTemplateDto;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseTemplateModuleHandler implements EcosModuleHandler<CaseTemplateDto>,
    ModuleChangesListener<CaseTemplateDto> {

    private static final String CASE_TEMPLATE_TYPE = "process/cmmn";

    private final CaseTemplateService caseTemplateService;
    private final CaseTemplateMapper caseTemplateMapper;

    private Consumer<CaseTemplateDto> changesListener;

    @Override
    public void deployModule(@NotNull CaseTemplateDto caseTemplateDTO) {
        log.info("Case template module received: " + caseTemplateDTO.getId());
        caseTemplateService.save(caseTemplateDTO);
    }

    @NotNull
    @Override
    public ModuleWithMeta<CaseTemplateDto> getModuleMeta(@NotNull CaseTemplateDto dto) {
        return new ModuleWithMeta<>(dto, new ModuleMeta(dto.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return CASE_TEMPLATE_TYPE;
    }

    @Override
    public void listenChanges(@NotNull Consumer<CaseTemplateDto> consumer) {
        this.changesListener = consumer;
    }

    @Nullable
    @Override
    public ModuleWithMeta<CaseTemplateDto> prepareToDeploy(@NotNull CaseTemplateDto caseTemplateDTO) {
        return getModuleMeta(caseTemplateDTO);
    }

    @Override
    public void perform(CaseTemplateDto dto) {
        changesListener.accept(dto);
    }
}
