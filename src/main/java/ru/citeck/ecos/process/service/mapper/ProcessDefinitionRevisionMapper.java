package ru.citeck.ecos.process.service.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.dto.ProcessDefinitionRevisionDto;

@Component
@RequiredArgsConstructor
public class ProcessDefinitionRevisionMapper {

    private final ModelMapper modelMapper;

    public ProcessDefinitionRevisionDto entityToDto(ProcessDefinitionRevision entity) {
        return modelMapper.map(entity, ProcessDefinitionRevisionDto.class);
    }

    public ProcessDefinitionRevision dtoToEntity(ProcessDefinitionRevisionDto dto) {
        return modelMapper.map(dto, ProcessDefinitionRevision.class);
    }
}
