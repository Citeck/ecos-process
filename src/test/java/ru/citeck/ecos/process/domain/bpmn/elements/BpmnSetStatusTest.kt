package ru.citeck.ecos.process.domain.bpmn.elements

import org.camunda.bpm.scenario.ProcessScenario
import org.camunda.bpm.scenario.Scenario.run
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.CamundaStatusSetter
import ru.citeck.ecos.process.domain.saveAndDeployBpmn
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension


/**
 * @author Roman Makarskiy
 */
@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnSetStatusTest {

    @Mock
    private lateinit var process: ProcessScenario

    @MockBean
    private lateinit var statusSetter: CamundaStatusSetter

    @Test
    fun `set status of document`() {
        val procId = "test-set-status"
        saveAndDeployBpmn("status", procId)

        val docRef = RecordRef.valueOf("doc@1")

        run(process).startByKey(
            procId,
            mapOf(
                "documentRef" to docRef.toString()
            )
        ).execute()

        verify(process).hasFinished("end")
        verify(statusSetter, times(1)).setStatus(docRef, "approval")
    }

}
