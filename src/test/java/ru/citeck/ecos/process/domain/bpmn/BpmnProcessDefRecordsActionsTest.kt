package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
import org.camunda.bpm.engine.RepositoryService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcessService
import ru.citeck.ecos.process.domain.deleteAllProcDefinitions
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.saveBpmnWithAction
import ru.citeck.ecos.process.domain.saveBpmnWithActionAndReplaceDefinition
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnProcessDefRecordsActionsTest {

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var camundaRepositoryService: RepositoryService

    @AfterEach
    fun tearDown() {
        deleteAllProcDefinitions()

        camundaRepositoryService.createDeploymentQuery().list().forEach {
            camundaRepositoryService.deleteDeployment(it.id, true)
        }
    }

    @Test
    fun `save definition without action should save converted definition`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `save definition without action should not deploy to engine`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save definition with save action should save converted definition`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `save definition multiple times with same definition should not save new revision`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
    }

    @Test
    fun `save definition multiple times with different definition should save new revision`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)
        saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.SAVE,
            "StartProcessEvent" to "StartProcessEvent2"
        )

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(2)
    }

    @Test
    fun `save definition with save action should not deploy to engine`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save definition with deploy action should save converted definition`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DEPLOY)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `save definition with deploy action should deploy to engine`() {
        val procId = "regular-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DEPLOY)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).hasSize(1)
        assertThat(processDefinitions.first().key).isEqualTo(procId)
    }

    @Test
    fun `save draft definition without action should save raw definition`() {
        val procId = "draft-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.RAW)
    }

    @Test
    fun `save draft definition without action should not deploy to engine`() {
        val procId = "draft-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save draft definition with draft action should save raw definition`() {
        val procId = "draft-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.RAW)
    }

    @Test
    fun `save draft definition with draft action should not deploy to engine`() {
        val procId = "draft-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save draft definition multiple times with same definition should not save new revision`() {
        val procId = "draft-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
    }

    @Test
    fun `save draft definition multiple times with different definition should not save new revision`() {
        val procId = "draft-bpmn-process"
        saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)
        saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.DRAFT,
            "StartDraftProcessEvent" to "StartDraftProcessEvent2"
        )

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
    }

    @Test
    fun `save draft definition multiple times with different definition and users should save as new revision`() {
        val procId = "draft-bpmn-process"

        // save as admin
        AuthContext.runAsFull("admin", listOf("ROLE_ADMIN")) {
            saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)
        }

        // save as system
        saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.DRAFT,
            "StartDraftProcessEvent" to "StartDraftProcessEvent2"
        )

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(2)
    }

    @ParameterizedTest
    @ValueSource(strings = ["SAVE", "DEPLOY"])
    fun `save draft definition with not draft action should throw`(action: String) {
        val procId = "draft-bpmn-process"

        val defAction = if (action.isBlank()) {
            null
        } else {
            BpmnProcessDefActions.valueOf(action)
        }

        assertThrows<EcosBpmnElementDefinitionException> {
            saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, defAction)
        }
    }
}
