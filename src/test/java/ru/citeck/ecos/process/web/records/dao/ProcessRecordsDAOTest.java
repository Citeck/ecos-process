package ru.citeck.ecos.process.web.records.dao;


import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.domain.Process;
import ru.citeck.ecos.process.domain.ProcessDefinition;
import ru.citeck.ecos.process.domain.ProcessDefinitionRevision;
import ru.citeck.ecos.process.domain.ProcessRevision;
import ru.citeck.ecos.process.repository.ProcessDefinitionRepository;
import ru.citeck.ecos.process.repository.ProcessDefinitionRevisionRepository;
import ru.citeck.ecos.process.repository.ProcessRepository;
import ru.citeck.ecos.process.repository.ProcessRevisionRepository;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EprocApp.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class ProcessRecordsDAOTest {

    private static final String PROCESS_DAO_ID = "process";
    private static final String ID_DIVIDER = "@";
    private static final String MICROSERVICE_PREFIX = "eproc";
    private static final String MICROSERVICE_PREFIX_DIVIDER = "/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcessRepository processRepository;

    @Autowired
    private ProcessRevisionRepository revisionRepository;

    @Autowired
    private ProcessDefinitionRevisionRepository definitionRevisionRepository;

    @Autowired
    private ProcessDefinitionRepository definitionRepository;

    @Test
    void queryProcessByUUID() throws Exception {

        Process process = new Process();
        process.setTenant(1);
        process.setRecord("record");
        process.setActive(true);
        processRepository.save(process);

        ProcessRevision pr = new ProcessRevision("pr-some-data".getBytes(), process);
        revisionRepository.save(pr);

        ProcessDefinition pd = new ProcessDefinition();
        pd.setTenant(1);
        definitionRepository.save(pd);

        ProcessDefinitionRevision pdr = new ProcessDefinitionRevision("pdr-some-data".getBytes(), pd);
        definitionRevisionRepository.save(pdr);

        process.setRevision(pr);
        process.setDefinitionRevision(pdr);
        processRepository.save(process);


        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"record\": \"process@" + process.getId() + "\",\n" +
                "    \"attributes\": [\n" +
                "        \"tenant\",\n" +
                "        \"revisionId\",\n" +
                "        \"definitionRevId\",\n" +
                "        \"created\",\n" +
                "        \"modified\",\n" +
                "        \"active\",\n" +
                "        \"record\"\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(PROCESS_DAO_ID + ID_DIVIDER + process.getId())))
            .andExpect(jsonPath("$.attributes.tenant", is(process.getTenant().toString())))
            .andExpect(jsonPath("$.attributes.revisionId", is(process.getRevision().getId().toString())))
            .andExpect(jsonPath("$.attributes.definitionRevId", is(process.getDefinitionRevision().getId().toString())))
            .andExpect(jsonPath("$.attributes.created", is(process.getCreated().toString())))
            .andExpect(jsonPath("$.attributes.modified", is(process.getModified().toString())))
            .andExpect(jsonPath("$.attributes.active", is(String.valueOf(process.isActive()))))
            .andExpect(jsonPath("$.attributes.record", is(process.getRecord())));
    }

    @Test
    void queryAllProcesses() throws Exception {

        //  arrange

        Process process1 = new Process();
        process1.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        process1.setTenant(0);
        process1.setRecord("record");
        process1.setActive(true);
        processRepository.save(process1);

        ProcessRevision pr = new ProcessRevision("pr-some-data".getBytes(), process1);
        revisionRepository.save(pr);

        ProcessDefinition pd = new ProcessDefinition();
        pd.setTenant(1);
        definitionRepository.save(pd);

        ProcessDefinitionRevision pdr = new ProcessDefinitionRevision("pdr-some-data".getBytes(), pd);
        definitionRevisionRepository.save(pdr);

        process1.setRevision(pr);
        process1.setDefinitionRevision(pdr);
        processRepository.save(process1);

        //  act & assert

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"query\": {\n" +
                "        \"sourceId\": \"process\"\n" +
                "    },\n" +
                "    \"attributes\": [\n" +
                "        \"tenant\",\n" +
                "        \"revisionId\",\n" +
                "        \"definitionRevId\",\n" +
                "        \"created\",\n" +
                "        \"modified\",\n" +
                "        \"active\",\n" +
                "        \"record\"\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", is(1)))
            .andExpect(jsonPath("$.hasMore", is(false)))
            .andExpect(jsonPath("$.errors", is(empty())))
            .andExpect(jsonPath("$.records[0].id",
                is(MICROSERVICE_PREFIX + MICROSERVICE_PREFIX_DIVIDER + PROCESS_DAO_ID + ID_DIVIDER +
                    process1.getId())))
            .andExpect(jsonPath("$.records[0].attributes.tenant", is(process1.getTenant().toString())))
            .andExpect(jsonPath("$.records[0].attributes.definitionRevId",
                is(process1.getDefinitionRevision().getId().toString())))
            .andExpect(jsonPath("$.records[0].attributes.created",
                is(process1.getCreated().toString())))
            .andExpect(jsonPath("$.records[0].attributes.modified",
                is(process1.getModified().toString())))
            .andExpect(jsonPath("$.records[0].attributes.active",
                is(String.valueOf(process1.isActive()))))
            .andExpect(jsonPath("$.records[0].attributes.record",
                is(process1.getRecord())))
            .andExpect(jsonPath("$.records[0].attributes.revisionId",
                is(process1.getRevision().getId().toString())));
    }

    @Test
    void mutateSaveNewProcess() throws Exception {

        //  arrange

        ProcessDefinition pd = new ProcessDefinition();
        pd.setTenant(1);
        definitionRepository.save(pd);

        ProcessDefinitionRevision pdr = new ProcessDefinitionRevision("pdr-some-data".getBytes(), pd);
        definitionRevisionRepository.save(pdr);

        //  act

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/mutate")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"records\": [\n" +
                "        {\n" +
                "            \"id\": \"process@" + "11111111-1111-1111-1111-111111111111" + "\",\n" +
                "            \"attributes\": {\n" +
                "                \"tenant?str\": \"0\",\n" +
                "                \"record?str\": \"record\",\n" +
                "                \"definitionRevId\": \"" + pdr.getId() +  "\",\n" +
                "                \"active?bool\": \"true\"\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk());

        //  assert
        //  NOTE: we dont check id, because it will autogenerated in this case

        List<Process> storedProcesses = processRepository.findAll();
        Assert.assertEquals(1, storedProcesses.size());
        Process storedProcess = storedProcesses.get(0);
        Assert.assertEquals(0, (int)storedProcess.getTenant());
        Assert.assertEquals("record", storedProcess.getRecord());
        Assert.assertTrue(storedProcess.isActive());
        Assert.assertEquals(pdr.getId(), storedProcess.getDefinitionRevision().getId());
        Assert.assertEquals(1, (int)storedProcess.getRevision().getVersion());
    }

    @Test
    void mutateUpdateExistingProcess() throws Exception {

        //  arrange

        Process process1 = new Process();
        process1.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        process1.setTenant(0);
        process1.setRecord("record");
        process1.setActive(true);
        processRepository.save(process1);

        ProcessRevision pr = new ProcessRevision("pr-some-data".getBytes(), process1);
        pr.setVersion(123);
        revisionRepository.save(pr);

        ProcessDefinition pd = new ProcessDefinition();
        pd.setTenant(1);
        definitionRepository.save(pd);

        ProcessDefinitionRevision pdr = new ProcessDefinitionRevision("pdr-some-data".getBytes(), pd);
        definitionRevisionRepository.save(pdr);

        process1.setRevision(pr);
        process1.setDefinitionRevision(pdr);
        processRepository.save(process1);

        //  act

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/mutate")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"records\": [\n" +
                "        {\n" +
                "            \"id\": \"process@" + "11111111-1111-1111-1111-111111111111" + "\",\n" +
                "            \"attributes\": {\n" +
                "                \"tenant?str\": \"1\",\n" +
                "                \"record?str\": \"record123\",\n" +
                "                \"definitionRevId\": \"" + pdr.getId() +  "\",\n" +
                "                \"revisionId\": \"" + pr.getId() +  "\",\n" +
                "                \"active?bool\": \"false\"\n" +
                "            }\n" +
                "        }\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk());

        //  assert

        List<Process> storedProcesses = processRepository.findAll();
        Assert.assertEquals(1, storedProcesses.size());
        Process storedProcess = storedProcesses.get(0);
        Assert.assertEquals(UUID.fromString("11111111-1111-1111-1111-111111111111"), storedProcess.getId());
        Assert.assertEquals(1, (int)storedProcess.getTenant());
        Assert.assertEquals("record123", storedProcess.getRecord());
        Assert.assertFalse(storedProcess.isActive());
        Assert.assertEquals(pdr.getId(), storedProcess.getDefinitionRevision().getId());
        Assert.assertEquals(123, (int)storedProcess.getRevision().getVersion());
    }
}
