package ru.citeck.ecos.process.eapps.casetemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.apps.module.handler.EcosModuleHandler;
import ru.citeck.ecos.apps.module.handler.ModuleMeta;
import ru.citeck.ecos.apps.module.handler.ModuleWithMeta;
import ru.citeck.ecos.process.dto.NewProcessDefDto;
import ru.citeck.ecos.process.service.ProcessDefService;

import java.util.Collections;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class CaseTemplateModuleHandler implements EcosModuleHandler<CaseTemplateDto> {

    private static final String CASE_TEMPLATE_TYPE = "process/cmmn";

    private final ProcessDefService processService;

    @Override
    public void deployModule(@NotNull CaseTemplateDto caseTemplateDTO) {

        log.info("Case template module received: " + caseTemplateDTO.getId());

        NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
        newProcessDefDto.setType("cmmn");
        newProcessDefDto.setData(caseTemplateDTO.getXmlContent());
        newProcessDefDto.setEcosTypeRef(caseTemplateDTO.getTypeRef());
        newProcessDefDto.setFormat("xml");
        newProcessDefDto.setId(caseTemplateDTO.getId());

        processService.uploadProcDef(newProcessDefDto);
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

    }

    @Nullable
    @Override
    public ModuleWithMeta<CaseTemplateDto> prepareToDeploy(@NotNull CaseTemplateDto caseTemplateDTO) {
        return getModuleMeta(caseTemplateDTO);
    }
}
