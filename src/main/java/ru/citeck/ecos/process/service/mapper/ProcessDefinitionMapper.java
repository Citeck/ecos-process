package ru.citeck.ecos.process.service.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.ProcessDefinition;
import ru.citeck.ecos.process.dto.ProcessDefinitionDto;

@Component
@RequiredArgsConstructor
public class ProcessDefinitionMapper {

    private final ModelMapper modelMapper;

    public ProcessDefinitionDto entityToDto(ProcessDefinition entity) {
        return modelMapper.map(entity, ProcessDefinitionDto.class);
    }

    public ProcessDefinition dtoToEntity(ProcessDefinitionDto dto) {
        return modelMapper.map(dto, ProcessDefinition.class);
    }
}
