package ru.citeck.ecos.process.eapps.casetemplate.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.eapps.casetemplate.CaseTemplateDTO;

@RequiredArgsConstructor
public class CaseTemplateModelMapper implements CaseTemplateMapper {

    private final ModelMapper modelMapper;

    @Override
    public CaseTemplateEntity dtoToEntity(CaseTemplateDTO dto) {
        return modelMapper.map(dto, CaseTemplateEntity.class);
    }

    @Override
    public CaseTemplateDTO entityToDto(CaseTemplateEntity entity) {
        return modelMapper.map(entity, CaseTemplateDTO.class);
    }
}
