package ru.citeck.ecos.process.service.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.CaseTemplate;
import ru.citeck.ecos.process.dto.CaseTemplateDto;

@Component
@RequiredArgsConstructor
public class CaseTemplateMapper {

    private final ModelMapper modelMapper;

    public CaseTemplate dtoToEntity(CaseTemplateDto dto) {
        return modelMapper.map(dto, CaseTemplate.class);
    }

    public CaseTemplateDto entityToDto(CaseTemplate entity) {
        return modelMapper.map(entity, CaseTemplateDto.class);
    }
}
