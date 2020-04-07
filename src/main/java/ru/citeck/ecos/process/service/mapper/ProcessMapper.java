package ru.citeck.ecos.process.service.mapper;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.dto.ProcessDefinitionRevisionDto;
import ru.citeck.ecos.process.dto.ProcessDto;
import ru.citeck.ecos.process.dto.ProcessRevisionDto;
import ru.citeck.ecos.process.service.ProcessDefinitionRevisionService;
import ru.citeck.ecos.process.service.ProcessRevisionService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcessMapper {

    private final ModelMapper modelMapper;
    private final ProcessDefinitionRevisionService definitionRevisionService;
    private final ProcessDefinitionRevisionMapper definitionRevisionMapper;
    private final ProcessRevisionService revisionService;
    private final ProcessRevisionMapper revisionMapper;

    public ProcessDto entityToDto(Process entity) {
        ProcessDto processDto = modelMapper.map(entity, ProcessDto.class);

        ProcessRevision pr = entity.getRevision();
        if (pr != null && pr.getId() != null) {
            processDto.setRevisionId(pr.getId());
        }

        ProcessDefinitionRevision pdr = entity.getDefinitionRevision();
        if (pdr != null && pdr.getId() != null) {
            processDto.setDefinitionRevId(pdr.getId());
        }

        return processDto;
    }

    public Process dtoToEntity(ProcessDto dto) {
        Process process = modelMapper.map(dto, Process.class);

        UUID pdrId = dto.getDefinitionRevId();
        if (pdrId != null) {
            ProcessDefinitionRevisionDto pdrDto = definitionRevisionService.getById(pdrId);
            process.setDefinitionRevision(definitionRevisionMapper.dtoToEntity(pdrDto));
        }

        UUID prId = dto.getRevisionId();
        if (prId != null) {
            ProcessRevisionDto prDto = revisionService.getById(prId);
            process.setRevision(revisionMapper.dtoToEntity(prDto));
        }

        return process;
    }
}
