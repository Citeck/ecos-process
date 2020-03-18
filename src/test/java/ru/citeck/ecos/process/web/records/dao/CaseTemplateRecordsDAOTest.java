package ru.citeck.ecos.process.web.records.dao;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import ru.citeck.ecos.process.EprocApp;
import ru.citeck.ecos.process.domain.CaseTemplateEntity;
import ru.citeck.ecos.process.repository.CaseTemplateRepository;
import ru.citeck.ecos.records2.RecordRef;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = EprocApp.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CaseTemplateRecordsDAOTest {

    private static final String CASE_TEMPLATE_DAO_ID = "case-template";
    private static final String ID_DIVIDER = "@";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CaseTemplateRepository caseTemplateRepository;

    @Test
    void queryCaseTemplate() throws Exception {
        CaseTemplateEntity caseTemplate = new CaseTemplateEntity("testId",
            RecordRef.create("emodel@type", "testTypeId"), "test-content".getBytes());

        caseTemplateRepository.save(caseTemplate);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/records/query")
            .contentType("application/json")
            .header("Content-type", "application/json")
            .content("{\n" +
                "    \"record\": \"case-template@testId\",\n" +
                "    \"attributes\": [\n" +
                "        \"ecosTypeRef?str\",\n" +
                "        \"xmlContent?disp\"\n" +
                "    ]\n" +
                "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(CASE_TEMPLATE_DAO_ID + ID_DIVIDER + caseTemplate.getId())))
            .andExpect(jsonPath("$.attributes.ecosTypeRef?str", is(caseTemplate.getEcosTypeRef().toString())))
            .andExpect(jsonPath("$.attributes.xmlContent?disp", is(caseTemplate.getXmlContent())));
    }
}
