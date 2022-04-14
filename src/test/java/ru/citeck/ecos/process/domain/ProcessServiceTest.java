package ru.citeck.ecos.process.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import ru.citeck.ecos.commands.CommandsService;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.domain.proc.command.createproc.CreateProc;
import ru.citeck.ecos.process.domain.proc.command.createproc.CreateProcResp;
import ru.citeck.ecos.process.domain.proc.command.getprocstate.GetProcState;
import ru.citeck.ecos.process.domain.proc.command.getprocstate.GetProcStateResp;
import ru.citeck.ecos.process.domain.proc.command.updateprocstate.UpdateProcState;
import ru.citeck.ecos.process.domain.proc.command.updateprocstate.UpdateProcStateResp;
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto;
import ru.citeck.ecos.process.domain.procdef.command.finddef.FindProcDef;
import ru.citeck.ecos.process.domain.procdef.command.finddef.FindProcDefResp;
import ru.citeck.ecos.process.domain.procdef.command.getprocdefrev.GetProcDefRev;
import ru.citeck.ecos.process.domain.procdef.command.getprocdefrev.GetProcDefRevResp;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = EprocApp.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ProcessServiceTest {

    @Autowired
    private ProcDefService procDefService;

    @Autowired
    private CommandsService commandsService;

    @Test
    void test() {

        RecordRef ecosTypeRef = TypesDao.type0Ref;

        String alfType = "{http://www.citeck.ru/model/content/idocs/1.0}contractor";

        NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
        newProcessDefDto.setId("test-id");
        newProcessDefDto.setFormat("xml");
        newProcessDefDto.setProcType("cmmn");
        newProcessDefDto.setEcosTypeRef(ecosTypeRef);
        newProcessDefDto.setAlfType(alfType);
        byte[] procDefData = "one two three".getBytes(StandardCharsets.UTF_8);
        newProcessDefDto.setData(procDefData);

        procDefService.uploadProcDef(newProcessDefDto);

        FindProcDefResp findProcDefResp = commandsService.executeSync(new FindProcDef("cmmn", ecosTypeRef, null))
                                                      .getResultAs(FindProcDefResp.class);
        assertNotNull(findProcDefResp);
        assertEquals("test-id", findProcDefResp.getProcDefId());
        assertNotNull(findProcDefResp.getProcDefRevId());

        FindProcDefResp findProcDefResp2 = commandsService.executeSync(new FindProcDef("cmmn", null, Collections.singletonList(alfType)))
            .getResultAs(FindProcDefResp.class);
        assertEquals(findProcDefResp, findProcDefResp2);

        FindProcDefResp findProcDefResp3 = commandsService.executeSync(new FindProcDef("cmmn", TypesDao.type1Ref, null))
            .getResultAs(FindProcDefResp.class);
        assertEquals(findProcDefResp, findProcDefResp3);

        FindProcDefResp findProcDefResp4 = commandsService.executeSync(new FindProcDef("cmmn", TypesDao.type2Ref, null))
            .getResultAs(FindProcDefResp.class);
        assertEquals(findProcDefResp, findProcDefResp4);

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

    @Component
    public static class TypesDao implements RecordAttsDao {

        public static final String ID = "emodel/type";

        public static final String type0Id = "type0";
        public static final RecordRef type0Ref = RecordRef.valueOf(ID + "@" + type0Id);
        public static final String type1Id = "type1";
        public static final RecordRef type1Ref = RecordRef.valueOf(ID + "@" + type1Id);
        public static final String type2Id = "type2";
        public static final RecordRef type2Ref = RecordRef.valueOf(ID + "@" + type2Id);

        private final Map<String, Record> records = new HashMap<>();

        public TypesDao() {

            records.put(type2Id, new Record(type2Id, Arrays.asList(
                RecordRef.valueOf(getId() + "@" + type1Id),
                RecordRef.valueOf(getId() + "@" + type0Id)
            )));
            records.put(type1Id, new Record(type1Id, Collections.singletonList(
                RecordRef.valueOf(getId() + "@" + type0Id)))
            );
            records.put(type0Id, new Record(type0Id, Collections.emptyList()));
        }

        @Nullable
        @Override
        public Object getRecordAtts(@NotNull String s) {
            return this.records.get(s);
        }

        @NotNull
        @Override
        public String getId() {
            return ID;
        }

        @Data
        @AllArgsConstructor
        @NoArgsConstructor
        public static class Record {
            private String id;
            private List<RecordRef> parents = new ArrayList<>();
        }
    }
}
