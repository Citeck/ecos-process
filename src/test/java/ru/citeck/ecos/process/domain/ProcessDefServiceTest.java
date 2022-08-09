package ru.citeck.ecos.process.domain;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import ru.citeck.ecos.commands.CommandsService;
import ru.citeck.ecos.commons.data.MLText;
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.domain.cmmn.CmmnConstantsKt;
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
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto;
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef;
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.predicate.model.VoidPredicate;
import ru.citeck.ecos.webapp.api.entity.EntityRef;
import ru.citeck.ecos.webapp.lib.model.type.dto.TypeDef;
import ru.citeck.ecos.webapp.lib.model.type.registry.EcosTypesRegistry;
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(EcosSpringExtension.class)
@SpringBootTest(classes = EprocApp.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class ProcessDefServiceTest {

    public static final String type0Id = "type0";
    public static final RecordRef type0Ref = TypeUtils.getTypeRef(type0Id);
    public static final String type1Id = "type1";
    public static final RecordRef type1Ref = TypeUtils.getTypeRef(type1Id);
    public static final String type2Id = "type2";
    public static final RecordRef type2Ref = TypeUtils.getTypeRef(type2Id);

    @Autowired
    private ProcDefService procDefService;
    @Autowired
    private CommandsService commandsService;
    @Autowired
    private EcosTypesRegistry ecosTypeRegistry;

    @BeforeEach
    public void setUp() {
        List<ProcDefDto> definitions = procDefService.findAll(VoidPredicate.INSTANCE, Integer.MAX_VALUE, 0);

        definitions.forEach(d -> procDefService.delete(ProcDefRef.create(d.getProcType(), d.getId())));

        ecosTypeRegistry.setValue(type0Id, TypeDef.create().withId(type0Id).build());
        ecosTypeRegistry.setValue(type1Id, TypeDef.create()
            .withId(type1Id)
            .withParentRef(type0Ref)
            .build()
        );
        ecosTypeRegistry.setValue(type2Id, TypeDef.create()
            .withId(type2Id)
            .withParentRef(type1Ref)
            .build()
        );
    }

    @Test
    public void uploadProcDefContentTest() {

        RecordRef ecosTypeRef = type0Ref;

        String alfType = "{http://www.citeck.ru/model/content/idocs/1.0}contractor";

        byte[] procDefData = "one two three".getBytes(StandardCharsets.UTF_8);


        NewProcessDefDto newProcessDefDto = new NewProcessDefDto(
            "test-id",
            MLText.EMPTY,
            CmmnConstantsKt.CMMN_TYPE,
            "xml",
            alfType,
            ecosTypeRef,
            EntityRef.EMPTY,
            procDefData,
            null,
            true,
            false,
            EntityRef.EMPTY
        );

        procDefService.uploadProcDef(newProcessDefDto);

        FindProcDefResp findProcDefResp = commandsService.executeSync(new FindProcDef(CmmnConstantsKt.CMMN_TYPE,
            ecosTypeRef, null)).getResultAs(FindProcDefResp.class);
        assertNotNull(findProcDefResp);
        assertEquals("test-id", findProcDefResp.getProcDefId());
        assertNotNull(findProcDefResp.getProcDefRevId());

        FindProcDefResp findProcDefResp2 = commandsService.executeSync(new FindProcDef(CmmnConstantsKt.CMMN_TYPE, null, Collections.singletonList(alfType)))
            .getResultAs(FindProcDefResp.class);
        assertEquals(findProcDefResp, findProcDefResp2);

        FindProcDefResp findProcDefResp3 = commandsService.executeSync(new FindProcDef(CmmnConstantsKt.CMMN_TYPE, type1Ref, null))
            .getResultAs(FindProcDefResp.class);
        assertEquals(findProcDefResp, findProcDefResp3);

        FindProcDefResp findProcDefResp4 = commandsService.executeSync(new FindProcDef(CmmnConstantsKt.CMMN_TYPE, type2Ref, null))
            .getResultAs(FindProcDefResp.class);
        assertEquals(findProcDefResp, findProcDefResp4);

        GetProcDefRevResp getProcDefRev = commandsService.executeSync(new GetProcDefRev(CmmnConstantsKt.CMMN_TYPE, findProcDefResp.getProcDefRevId()))
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

        GetProcStateResp getProcStateResp = commandsService.executeSync(new GetProcState(CmmnConstantsKt.CMMN_TYPE, newStateResp.getProcStateId()))
            .getResultAs(GetProcStateResp.class);

        assertNotNull(getProcStateResp);
        assertEquals(1, getProcStateResp.getVersion());
        assertArrayEquals(stateData, getProcStateResp.getStateData());
    }
}
