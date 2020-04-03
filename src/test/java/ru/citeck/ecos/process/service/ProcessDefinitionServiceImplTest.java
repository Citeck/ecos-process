package ru.citeck.ecos.process.service;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.citeck.ecos.process.domain.ProcessDefinition;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.repository.ProcessDefinitionRepository;
import ru.citeck.ecos.process.dto.ProcessDefinitionDto;
import ru.citeck.ecos.process.repository.ProcessDefinitionRevisionRepository;
import ru.citeck.ecos.process.service.impl.ProcessDefinitionServiceImpl;
import ru.citeck.ecos.process.service.mapper.ProcessDefinitionMapper;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class ProcessDefinitionServiceImplTest {

    private ProcessDefinitionServiceImpl processDefinitionService;

    @MockBean
    private ProcessDefinitionRepository repository;

    @MockBean
    private ProcessDefinitionMapper mapper;

    @MockBean
    private ProcessDefinitionRevisionRepository definitionRevisionRepository;

    @BeforeEach
    void setUp() {
        processDefinitionService = new ProcessDefinitionServiceImpl(repository, mapper, definitionRevisionRepository);
    }

    @Test
    void testSaveWithCreateFlow() {
        //  arrange
        UUID revId = UUID.randomUUID();

        ProcessDefinitionRevision pdr = new ProcessDefinitionRevision();

        ProcessDefinition receivedEntity = new ProcessDefinition();
        receivedEntity.setId("procDefId");
        receivedEntity.setModified(null);
        receivedEntity.setTenant(1);
        receivedEntity.setDefinitionRevision(pdr);

        ProcessDefinition savedEntity = new ProcessDefinition();
        savedEntity.setId("NEW_RANDOM_GENERATED_UUID");
        savedEntity.setModified(savedEntity.getCreated());
        savedEntity.setTenant(1);
        savedEntity.setDefinitionRevision(pdr);

        ProcessDefinitionDto dto = new ProcessDefinitionDto("procDefId", 1, revId);
        ProcessDefinitionDto savedDto = new ProcessDefinitionDto("NEW_RANDOM_GENERATED_UUID", 1, revId);

        when(repository.findById(dto.getId())).thenReturn(Optional.empty());
        when(mapper.dtoToEntity(dto)).thenReturn(receivedEntity);
        when(repository.save(receivedEntity)).thenReturn(savedEntity);
        when(mapper.entityToDto(savedEntity)).thenReturn(savedDto);

        //  act
        ProcessDefinitionDto resultDto = processDefinitionService.save(dto);

        //  assert
        Assert.assertEquals(resultDto, savedDto);
        Assert.assertEquals(resultDto.getId(), savedDto.getId());
        Assert.assertEquals(resultDto.getRevisionId(), savedDto.getRevisionId());
        Assert.assertEquals(resultDto.getTenant(), savedDto.getTenant());
    }

    @Test
    void testSaveWithUpdateFlow() {
        //  arrange
        UUID updatedRevId = UUID.randomUUID();

        ProcessDefinitionRevision pdr = new ProcessDefinitionRevision();

        ProcessDefinition receivedEntity = new ProcessDefinition();
        receivedEntity.setId("procDefId");
        receivedEntity.setModified(null);
        receivedEntity.setTenant(1);
        receivedEntity.setDefinitionRevision(pdr);

        ProcessDefinition storedEntity = new ProcessDefinition();
        storedEntity.setId("procDefId");
        storedEntity.setModified(LocalDateTime.of(2020, 4, 10, 19, 40));
        storedEntity.setTenant(1);
        storedEntity.setDefinitionRevision(pdr);

        ProcessDefinitionDto dto = new ProcessDefinitionDto("procDefId", 1, pdr.getId());
        ProcessDefinitionDto savedDto = new ProcessDefinitionDto("procDefId", 2, updatedRevId);

        when(repository.findById(dto.getId())).thenReturn(Optional.of(storedEntity));
        when(mapper.dtoToEntity(dto)).thenReturn(receivedEntity);
        when(repository.save(storedEntity)).thenReturn(storedEntity);
        when(mapper.entityToDto(storedEntity)).thenReturn(savedDto);
        when(definitionRevisionRepository.findById(pdr.getId())).thenReturn(Optional.of(pdr));

        //  act
        ProcessDefinitionDto resultDto = processDefinitionService.save(dto);

        //  assert
        Assert.assertEquals(resultDto, savedDto);
        Assert.assertEquals(resultDto.getId(), savedDto.getId());
        Assert.assertEquals(resultDto.getRevisionId(), savedDto.getRevisionId());
        Assert.assertEquals(resultDto.getTenant(), savedDto.getTenant());
    }
}
