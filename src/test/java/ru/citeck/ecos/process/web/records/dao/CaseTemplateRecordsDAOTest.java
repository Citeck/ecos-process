package ru.citeck.ecos.process.web.records.dao;


import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.domain.CaseTemplate;
import ru.citeck.ecos.process.repository.CaseTemplateRepository;
import ru.citeck.ecos.records2.RecordRef;

import java.util.Arrays;
import java.util.Base64;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EprocApp.class)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ActiveProfiles("test")
public class CaseTemplateRecordsDAOTest {

    private static final String CASE_TEMPLATE_DAO_ID = "case-template";
    private static final String ID_DIVIDER = "@";
    private static final String MICROSERVICE_PREFIX = "eproc";
    private static final String MICROSERVICE_PREFIX_DIVIDER = "/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseTemplateRepository caseTemplateRepository;

    @Test
    void queryCaseTemplate() throws Exception {
        CaseTemplate caseTemplate = new CaseTemplate("testId",
            RecordRef.create("emodel@type", "testTypeId"), "test-content".getBytes());
        String xmlContentBase64Str = Base64.getEncoder().encodeToString(caseTemplate.getXmlContent());
        caseTemplateRepository.save(caseTemplate);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"record\": \"case-template@testId\",\n" +
                "    \"attributes\": [\n" +
                "        \"typeRef\",\n" +
                "        \"xmlContent\"\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(CASE_TEMPLATE_DAO_ID + ID_DIVIDER + caseTemplate.getId())))
            .andExpect(jsonPath("$.attributes.typeRef", is(caseTemplate.getTypeRef().toString())))
            .andExpect(jsonPath("$.attributes.xmlContent", is(xmlContentBase64Str)));
    }

    @Test
    void queryAllCaseTemplates() throws Exception {
        CaseTemplate caseTemplate = new CaseTemplate("testId",
            RecordRef.create("emodel@type", "testTypeId"), "test-content".getBytes());
        CaseTemplate caseTemplate2 = new CaseTemplate("testId2",
            RecordRef.create("emodel@type", "testTypeId"), "test-content2".getBytes());

        String xmlContentBase64Str = Base64.getEncoder().encodeToString(caseTemplate.getXmlContent());
        String xmlContent2Base64Str = Base64.getEncoder().encodeToString(caseTemplate2.getXmlContent());

        caseTemplateRepository.saveAll(Arrays.asList(caseTemplate, caseTemplate2));

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"query\": {\n" +
                "        \"sourceId\": \"case-template\"\n" +
                "    },\n" +
                "    \"attributes\": {\n" +
                "        \"typeRef\": \"typeRef\",\n" +
                "        \"xmlContent\": \"xmlContent\"\n" +
                "    }\n" +
                "}\n"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalCount", is(2)))
            .andExpect(jsonPath("$.hasMore", is(false)))
            .andExpect(jsonPath("$.errors", is(empty())))
            .andExpect(jsonPath("$.records[0].id", is(MICROSERVICE_PREFIX + MICROSERVICE_PREFIX_DIVIDER +
                CASE_TEMPLATE_DAO_ID + ID_DIVIDER + caseTemplate.getId())))
            .andExpect(jsonPath("$.records[1].id", is(MICROSERVICE_PREFIX + MICROSERVICE_PREFIX_DIVIDER +
                CASE_TEMPLATE_DAO_ID + ID_DIVIDER + caseTemplate2.getId())))
            .andExpect(jsonPath("$.records[0].attributes.typeRef", is(caseTemplate.getTypeRef().toString())))
            .andExpect(jsonPath("$.records[1].attributes.typeRef", is(caseTemplate2.getTypeRef().toString())))
            .andExpect(jsonPath("$.records[0].attributes.xmlContent", is(xmlContentBase64Str)))
            .andExpect(jsonPath("$.records[1].attributes.xmlContent", is(xmlContent2Base64Str)));

    }
}
