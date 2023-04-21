package ru.citeck.ecos.process.domain.dmn

import org.apache.commons.lang3.LocaleUtils
import org.camunda.bpm.engine.RepositoryService
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.apps.app.service.LocalAppService
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_RECOURSE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DMN_TEST_DEF_ID = "dmn-test-artifact-handler"
private const val DMN_TEST_DECISION_ID = "decision-test-deploy"

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DmnArtifactHandlerTest {

    @Autowired
    private lateinit var localAppService: LocalAppService

    @Autowired
    private lateinit var camundaRepoService: RepositoryService

    @Autowired
    private lateinit var procDefService: ProcDefService

    @BeforeAll
    fun setUp() {
        AuthContext.runAsSystem {
            localAppService.deployLocalArtifacts()
        }
    }

    @Test
    fun `check definition meta data of loaded dmn from artifact`() {
        val deployed = procDefService.getProcessDefById(ProcDefRef.create(DMN_PROC_TYPE, DMN_TEST_DEF_ID))

        assertNotNull(deployed)

        assertEquals(DMN_TEST_DEF_ID, deployed.id)
        assertEquals(
            MLText(
                mapOf(
                    LocaleUtils.toLocale("ru") to "Тест deploy",
                    LocaleUtils.toLocale("en") to "Test deploy"
                )
            ),
            deployed.name
        )

        assertEquals(DMN_FORMAT, deployed.format)
        assertEquals(DMN_PROC_TYPE, deployed.procType)

        assertNull(deployed.alfType)

        assertTrue(deployed.data != null && deployed.data.isNotEmpty())
    }

    @Test
    fun `loaded dmn decision from artifact should be deployed to engine`() {
        val dmnDecisions = camundaRepoService
            .createDecisionDefinitionQuery()
            .decisionDefinitionKey(DMN_TEST_DECISION_ID)
            .list()

        assertEquals(1, dmnDecisions.size)
        assertEquals(DMN_TEST_DECISION_ID, dmnDecisions.first().key)
        assertEquals(DMN_TEST_DEF_ID + DMN_RECOURSE_NAME_POSTFIX, dmnDecisions.first().resourceName)
        assertEquals(1, dmnDecisions.first().version)
    }
}
