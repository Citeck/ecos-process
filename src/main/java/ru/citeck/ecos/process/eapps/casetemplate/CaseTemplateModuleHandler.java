package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.eapps.casetemplate.mapper.CaseTemplateMapper;
import ru.citeck.ecos.process.service.CaseTemplateService;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseTemplateModuleHandler implements EcosModuleHandler<CaseTemplateDTO>, ListenModuleChanges<CaseTemplateDTO> {

    private static final String CASE_TEMPLATE_TYPE = "case-template";

    private final CaseTemplateService caseTemplateService;
    private final CaseTemplateMapper caseTemplateMapper;

    private Consumer<CaseTemplateDTO> changesListener;

    @Override
    public void deployModule(@NotNull CaseTemplateDTO caseTemplateDTO) {
        log.info("Case template module received: " + caseTemplateDTO.getId());
        CaseTemplateEntity entity = caseTemplateMapper.dtoToEntity(caseTemplateDTO);
        caseTemplateService.save(entity);
    }

    @NotNull
    @Override
    public ModuleWithMeta<CaseTemplateDTO> getModuleMeta(@NotNull CaseTemplateDTO dto) {
        return new ModuleWithMeta<>(dto, new ModuleMeta(dto.getId(), Collections.emptyList()));
    }

    @NotNull
    @Override
    public String getModuleType() {
        return CASE_TEMPLATE_TYPE;
    }

    @Override
    public void listenChanges(@NotNull Consumer<CaseTemplateDTO> consumer) {
        this.changesListener = consumer;
    }

    @Nullable
    @Override
    public ModuleWithMeta<CaseTemplateDTO> prepareToDeploy(@NotNull CaseTemplateDTO caseTemplateDTO) {
        return getModuleMeta(caseTemplateDTO);
    }

    @Override
    public void perform(CaseTemplateEntity entity) {
        CaseTemplateDTO dto = caseTemplateMapper.entityToDto(entity);
        changesListener.accept(dto);
    }
}
