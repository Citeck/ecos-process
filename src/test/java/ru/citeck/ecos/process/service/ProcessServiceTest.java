package ru.citeck.ecos.process.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import ru.citeck.ecos.commands.CommandsService;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.dto.NewProcessDefDto;
import ru.citeck.ecos.process.service.commands.createproc.CreateProc;
import ru.citeck.ecos.process.service.commands.createproc.CreateProcResp;
import ru.citeck.ecos.process.service.commands.finddef.FindProcDef;
import ru.citeck.ecos.process.service.commands.finddef.FindProcDefResp;
import ru.citeck.ecos.process.service.commands.getprocdefrev.GetProcDefRev;
import ru.citeck.ecos.process.service.commands.getprocdefrev.GetProcDefRevResp;
import ru.citeck.ecos.process.service.commands.getprocstate.GetProcState;
import ru.citeck.ecos.process.service.commands.getprocstate.GetProcStateResp;
import ru.citeck.ecos.process.service.commands.updateprocstate.UpdateProcState;
import ru.citeck.ecos.process.service.commands.updateprocstate.UpdateProcStateResp;
import ru.citeck.ecos.records2.RecordRef;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EprocApp.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class ProcessServiceTest {

    @Autowired
    private ProcessDefService processDefService;

    @Autowired
    private CommandsService commandsService;

    @Test
    void test() {

        RecordRef ecosTypeRef = RecordRef.create("emodel", "type", "local");

        NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
        newProcessDefDto.setId("test-id");
        newProcessDefDto.setFormat("xml");
        newProcessDefDto.setType("cmmn");
        newProcessDefDto.setEcosTypeRef(ecosTypeRef);
        byte[] procDefData = "one two three".getBytes(StandardCharsets.UTF_8);
        newProcessDefDto.setData(procDefData);

        processDefService.uploadProcDef(newProcessDefDto);

        FindProcDefResp findProcDefResp = commandsService.executeSync(new FindProcDef("cmmn", ecosTypeRef))
                                                      .getResultAs(FindProcDefResp.class);
        assertNotNull(findProcDefResp);
        assertEquals("test-id", findProcDefResp.getProcDefId());
        assertNotNull(findProcDefResp.getProcDefRevId());

        GetProcDefRevResp getProcDefRev = commandsService.executeSync(new GetProcDefRev("cmmn", findProcDefResp.getProcDefRevId()))
            .getResultAs(GetProcDefRevResp.class);

        assertNotNull(getProcDefRev);
        assertArrayEquals(procDefData, getProcDefRev.getData());
        assertEquals(newProcessDefDto.getFormat(), getProcDefRev.getFormat());
        assertEquals(newProcessDefDto.getId(), getProcDefRev.getProcDefId());

        RecordRef docRef = RecordRef.valueOf("uiserv/test@local");
        CreateProcResp newProcResp = commandsService.executeSync(new CreateProc(findProcDefResp.getProcDefRevId(), docRef))
                        .getResultAs(CreateProcResp.class);

        assertNotNull(newProcResp);
        assertFalse(StringUtils.isBlank(newProcResp.getProcId()));
        assertFalse(StringUtils.isBlank(newProcResp.getProcStateId()));
        assertNotNull(newProcResp.getProcStateData());

        byte[] stateData = "process state data".getBytes(StandardCharsets.UTF_8);

        UpdateProcStateResp newStateResp = commandsService.executeSync(new UpdateProcState(newProcResp.getProcStateId(), stateData))
            .getResultAs(UpdateProcStateResp.class);

        assertNotNull(newStateResp);
        assertFalse(StringUtils.isBlank(newStateResp.getProcStateId()));
        assertEquals(1, newStateResp.getVersion());

        GetProcStateResp getProcStateResp = commandsService.executeSync(new GetProcState("cmmn", newStateResp.getProcStateId()))
            .getResultAs(GetProcStateResp.class);

        assertNotNull(getProcStateResp);
        assertEquals(1, getProcStateResp.getVersion());
        assertArrayEquals(stateData, getProcStateResp.getStateData());
    }
}
