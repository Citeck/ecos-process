package ru.citeck.ecos.process.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.stereotype.Component;
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
import ru.citeck.ecos.records2.graphql.meta.value.MetaField;
import ru.citeck.ecos.records2.source.dao.local.LocalRecordsDAO;
import ru.citeck.ecos.records2.source.dao.local.v2.LocalRecordsMetaDAO;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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

        RecordRef ecosTypeRef = TypesDao.type0Ref;

        String alfType = "{http://www.citeck.ru/model/content/idocs/1.0}contractor";

        NewProcessDefDto newProcessDefDto = new NewProcessDefDto();
        newProcessDefDto.setId("test-id");
        newProcessDefDto.setFormat("xml");
        newProcessDefDto.setType("cmmn");
        newProcessDefDto.setEcosTypeRef(ecosTypeRef);
        newProcessDefDto.setAlfType(alfType);
        byte[] procDefData = "one two three".getBytes(StandardCharsets.UTF_8);
        newProcessDefDto.setData(procDefData);

        processDefService.uploadProcDef(newProcessDefDto);

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
    public static class TypesDao extends LocalRecordsDAO implements LocalRecordsMetaDAO<Object> {

        public static final String ID = "emodel/type";

        public static final String type0Id = "type0";
        public static final RecordRef type0Ref = RecordRef.valueOf(ID + "@" + type0Id);
        public static final String type1Id = "type1";
        public static final RecordRef type1Ref = RecordRef.valueOf(ID + "@" + type1Id);
        public static final String type2Id = "type2";
        public static final RecordRef type2Ref = RecordRef.valueOf(ID + "@" + type2Id);

        private Map<String, Record> records = new HashMap<>();

        public TypesDao() {
            setId(ID);

            records.put(type2Id, new Record(type2Id, Arrays.asList(
                RecordRef.valueOf(getId() + "@" + type1Id),
                RecordRef.valueOf(getId() + "@" + type0Id)
            )));
            records.put(type1Id, new Record(type1Id, Collections.singletonList(
                RecordRef.valueOf(getId() + "@" + type0Id)))
            );
            records.put(type0Id, new Record(type0Id, Collections.emptyList()));
        }

        @Override
        public List<Object> getLocalRecordsMeta(List<RecordRef> records, MetaField metaField) {
            return records.stream().map(r -> this.records.get(r.getId())).collect(Collectors.toList());
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
