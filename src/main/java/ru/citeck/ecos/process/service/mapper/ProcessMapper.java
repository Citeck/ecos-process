package ru.citeck.ecos.process.service.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.dto.ProcessDto;

@Component
@RequiredArgsConstructor
public class ProcessMapper {

    private final ModelMapper modelMapper;

    public ProcessDto entityToDto(Process entity) {
        return modelMapper.map(entity, ProcessDto.class);
    }

    public Process dtoToEntity(ProcessDto dto) {
        return modelMapper.map(dto, Process.class);
    }
}
