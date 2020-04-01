package ru.citeck.ecos.process.service.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.dto.ProcessDto;
import ru.citeck.ecos.process.dto.ProcessRevisionDto;

@Component
@RequiredArgsConstructor
public class ProcessRevisionMapper {

    private final ModelMapper modelMapper;

    public ProcessRevisionDto entityToDto(ProcessRevision entity) {
        return modelMapper.map(entity, ProcessRevisionDto.class);
    }

    public ProcessRevision dtoToEntity(ProcessRevisionDto dto) {
        return modelMapper.map(dto, ProcessRevision.class);
    }
}
