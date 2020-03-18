package ru.citeck.ecos.process.eapps.casetemplate.mapper;

import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.eapps.casetemplate.CaseTemplateDTO;

public interface CaseTemplateMapper {

    CaseTemplateEntity dtoToEntity(CaseTemplateDTO dto);
    CaseTemplateDTO entityToDto(CaseTemplateEntity entity);

}
