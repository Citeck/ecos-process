package ru.citeck.ecos.process.service.impl;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import ru.citeck.ecos.commands.dto.CommandResult;
import ru.citeck.ecos.commons.data.ObjectData;
import ru.citeck.ecos.commons.json.Json;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.domain.EntityUuid;
import ru.citeck.ecos.process.domain.TimerEntity;
import ru.citeck.ecos.process.dto.TimerCommandDto;
import ru.citeck.ecos.process.dto.TimerDto;
import ru.citeck.ecos.process.repository.TimerRepository;
import ru.citeck.ecos.process.service.TimerService;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommand;
import ru.citeck.ecos.process.service.commands.createtimer.CreateTimerCommandRes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = EprocApp.class)
@ActiveProfiles("test")
public class TimerServiceImplTest {

    private static final String COMMAND_RESULT_JSON = "{\n" +
        "    \"id\": \"id\",\n" +
        "    \"started\": 1,\n" +
        "    \"completed\": 2,\n" +
        "    \"command\": {\n" +
        "        \"id\": \"id\",\n" +
        "        \"tenant\": \"tenant\",\n" +
        "        \"time\": 1,\n" +
        "        \"targetApp\": \"targetApp\",\n" +
        "        \"user\": \"user\",\n" +
        "        \"sourceApp\": \"sourceApp\",\n" +
        "        \"sourceAppId\": \"sourceAppId\",\n" +
        "        \"type\": \"type\",\n" +
        "        \"body\": {\n" +
        "            \"node\": \"node\"\n" +
        "        },\n" +
        "        \"transaction\": \"REQUIRED\"\n" +
        "    },\n" +
        "    \"appName\": \"appName\",\n" +
        "    \"appInstanceId\": \"appInstanceId\",\n" +
        "    \"result\": {\n" +
        "        \"node\": \"node\"\n" +
        "    },\n" +
        "    \"errors\": [\n" +
        "        {\n" +
        "            \"type\": \"type\",\n" +
        "            \"message\": \"message\",\n" +
        "            \"stackTrace\": [\n" +
        "                \"stackTrace\"\n" +
        "            ]\n" +
        "        }\n" +
        "    ]\n" +
        "}";


    @Autowired
    private TimerRepository timerRepository;

    @Autowired
    private TimerService timerService;

    @MockBean
    private ProcessTenantService tenantService;

    @Before
    public void before() {
        timerRepository.deleteAll();
    }

    @Test
    public void createTimer_returnsProperResult() {
        Instant triggerTime = Instant.now();

        String commandId = "id";
        String targetApp = "targetApp";
        String type = "type";
        String body = "{\"body\":\"body\"}";

        TimerCommandDto commandDto = new TimerCommandDto();
        commandDto.setId(commandId);
        commandDto.setTargetApp(targetApp);
        commandDto.setType(type);
        commandDto.setBody(Json.getMapper().read(body, ObjectData.class));

        CreateTimerCommand createTimerCommand = new CreateTimerCommand();

        createTimerCommand.setTriggerTime(triggerTime);
        createTimerCommand.setCommand(commandDto);

        int tenant = 10;

        when(tenantService.getCurrent()).thenReturn(tenant);

        CreateTimerCommandRes saved = timerService.createTimer(createTimerCommand);

        List<TimerEntity> entities = timerRepository.findAll();
        assertEquals(1, entities.size());

        TimerEntity entity = entities.get(0);

        assertNotNull(entity.getId());
        assertEquals((Integer) tenant, entity.getId().getTnt());
        assertEquals(saved.getTimerId(), entity.getId().getId().toString());

        assertEquals(triggerTime, entity.getTriggerTime());
        assertTrue(entity.isActive());

        TimerCommandDto timerCommandDto = Json.getMapper().read(entity.getCommand(), TimerCommandDto.class);

        assertNotNull(timerCommandDto);
        assertEquals(commandId, timerCommandDto.getId());
        assertEquals(targetApp, timerCommandDto.getTargetApp());
        assertEquals(type, timerCommandDto.getType());
        assertEquals(new ObjectData(body), timerCommandDto.getBody());
        assertNull(entity.getResult());
    }

    @Test
    public void save_properUpdate() {

        UUID id = UUID.randomUUID();
        int tenant = 5;

        TimerEntity entity = new TimerEntity();
        entity.setId(new EntityUuid(tenant, id));

        timerRepository.save(entity);

        int retryCount = 10;
        Instant now = Instant.now();
        Instant triggerTime = now.minus(10, ChronoUnit.MINUTES);
        Boolean active = Boolean.FALSE;

        String commandId = "id";
        String targetApp = "targetApp";
        String type = "type";
        String body = "{\"body\":\"body\"}";
        TimerCommandDto timerCommandDto = new TimerCommandDto();
        timerCommandDto.setId(commandId);
        timerCommandDto.setTargetApp(targetApp);
        timerCommandDto.setType(type);
        timerCommandDto.setBody(new ObjectData(body));

        CommandResult result = Json.getMapper().read(COMMAND_RESULT_JSON, CommandResult.class);

        TimerDto dto = new TimerDto();
        dto.setId(id);
        dto.setRetryCounter(retryCount);
        dto.setTriggerTime(triggerTime);
        dto.setActive(active);
        dto.setCommand(timerCommandDto);
        dto.setResult(result);

        timerService.save(dto);

        List<TimerEntity> entities = timerRepository.findAll();
        assertEquals(1, entities.size());

        TimerEntity saved = entities.get(0);
        assertEquals(new EntityUuid(tenant, id), saved.getId());
        assertEquals((Integer) retryCount, saved.getRetryCounter());
        assertEquals(triggerTime, saved.getTriggerTime());
        assertEquals(active, saved.isActive());
        assertNotNull(saved.getCommand());

        TimerCommandDto command = Json.getMapper().read(saved.getCommand(), TimerCommandDto.class);

        assertEquals(commandId, command.getId());
        assertEquals(targetApp, command.getTargetApp());
        assertEquals(type, command.getType());
        assertEquals(new ObjectData(body), command.getBody());
        assertEquals(Json.getMapper().read(COMMAND_RESULT_JSON, CommandResult.class),
            Json.getMapper().read(saved.getResult(), CommandResult.class));
    }
}
