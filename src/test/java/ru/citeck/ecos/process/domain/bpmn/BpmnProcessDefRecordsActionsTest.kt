package ru.citeck.ecos.process.domain.bpmn

import org.assertj.core.api.Assertions.assertThat
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
import ru.citeck.ecos.process.domain.*
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefVersionRecords
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_PROCESS_DEF_ID
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnProcessDefRecordsActionsTest {

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var procDefRevDataProvider: ProcDefRevDataProvider

    @Autowired
    private lateinit var helper: BpmnProcHelper

    @AfterEach
    fun tearDown() {
        helper.cleanDeployments()
        helper.cleanDefinitions()
    }

    @Test
    fun `save definition without action should save converted definition`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `save definition without action should not deploy to engine`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save definition with save action should save converted definition`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `save definition multiple times with same definition should not save new revision`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
    }

    @Test
    fun `save definition multiple times with different definition should save new revision`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)
        helper.saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.SAVE,
            "StartProcessEvent" to "StartProcessEvent2"
        )

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(2)
    }

    @Test
    fun `save definition with save action should not deploy to engine`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.SAVE)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save definition with deploy action should save converted definition`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DEPLOY)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `save definition with deploy action should deploy to engine`() {
        val procId = "regular-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DEPLOY)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).hasSize(1)
        assertThat(processDefinitions.first().key).isEqualTo(procId)
    }

    @Test
    fun `save draft definition without action should save raw definition`() {
        val procId = "draft-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.RAW)
    }

    @Test
    fun `save draft definition without action should not deploy to engine`() {
        val procId = "draft-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save draft definition with draft action should save raw definition`() {
        val procId = "draft-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.RAW)
    }

    @Test
    fun `save draft definition with draft action should not deploy to engine`() {
        val procId = "draft-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)

        val processDefinitions = bpmnProcessService.getProcessDefinitionsByKey(procId)

        assertThat(processDefinitions).isEmpty()
    }

    @Test
    fun `save draft definition multiple times with same definition should not save new revision`() {
        val procId = "draft-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
    }

    @Test
    fun `save draft definition multiple times with different definition should not save new revision`() {
        val procId = "draft-bpmn-process"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)
        helper.saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.DRAFT,
            "StartDraftProcessEvent" to "StartDraftProcessEvent2"
        )

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

        assertThat(revisions).hasSize(1)
    }

    @Test
    fun `save draft definition multiple times with different definition and users should save as new revision`() {
        val procId = "draft-bpmn-process"

        // save as admin
        AuthContext.runAsFull("admin", listOf("ROLE_ADMIN")) {
            helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, BpmnProcessDefActions.DRAFT)
        }

        // save as system
        helper.saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            BpmnProcessDefActions.DRAFT,
            "StartDraftProcessEvent" to "StartDraftProcessEvent2"
        )

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))

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
            helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, defAction)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["test-copy", "test-copy-draft"])
    fun `copy bpmn process test`(procId: String) {
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.copyBpmnModule(procId, copiedId)

        val originalDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
            ?: error("Original definition not found")
        val copiedDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))
            ?: error("Copied definition not found")

        assertThat(copiedDef.id).isEqualTo(copiedId)
        assertThat(copiedDef.formRef).isEqualTo(originalDef.formRef)
        assertThat(copiedDef.ecosTypeRef).isEqualTo(originalDef.ecosTypeRef)
    }

    @ParameterizedTest
    @ValueSource(strings = ["test-copy", "test-copy-draft"])
    fun `copy bpmn process should save working copy source ref`(procId: String) {
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.copyBpmnModule(procId, copiedId)

        val originalDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
            ?: error("Original definition not found")
        val copiedDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))
            ?: error("Copied definition not found")

        assertThat(copiedDef.workingCopySourceRef).isEqualTo(
            EntityRef.create(
                AppName.EPROC,
                BpmnProcessDefVersionRecords.ID,
                originalDef.revisionId.toString()
            )
        )
    }

    @Test
    fun `copy bpmn process with multiple revisions should save last working copy source ref`() {
        val procId = "test-copy"
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.saveBpmnWithActionAndReplaceDefinition(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            null,
            "Event_1n4clhz" to "Event_some_event"
        )

        helper.copyBpmnModule(procId, copiedId)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
        assertThat(revisions.size).isEqualTo(2)

        val originalDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
            ?: error("Original definition not found")
        val copiedDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))
            ?: error("Copied definition not found")

        assertThat(copiedDef.workingCopySourceRef).isEqualTo(
            EntityRef.create(
                AppName.EPROC,
                BpmnProcessDefVersionRecords.ID,
                originalDef.revisionId.toString()
            )
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["test-copy", "test-copy-draft"])
    fun `copied bpmn process should have disabled auto start`(procId: String) {
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.copyBpmnModule(procId, copiedId)

        val originalDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
            ?: error("Original definition not found")
        val copiedDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))
            ?: error("Copied definition not found")

        assertThat(originalDef.enabled).isTrue()
        assertThat(originalDef.autoStartEnabled).isTrue()

        assertThat(copiedDef.enabled).isFalse()
        assertThat(copiedDef.autoStartEnabled).isFalse()
    }

    @Test
    fun `copied bpmn process should have converted state`() {
        val procId = "test-copy"
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.copyBpmnModule(procId, copiedId)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.CONVERTED)
    }

    @Test
    fun `copied draft bpmn process should have draft state`() {
        val procId = "test-copy-draft"
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.copyBpmnModule(procId, copiedId)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))

        assertThat(revisions).hasSize(1)
        assertThat(revisions.first().dataState).isEqualTo(ProcDefRevDataState.RAW)
    }

    @ParameterizedTest
    @ValueSource(strings = ["test-new-version", "test-new-version-draft"])
    fun `upload new version definition test`(procId: String) {
        val comment = "New version comment"
        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)

        val revisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
        assertThat(revisions).hasSize(1)
        assertThat(revisions[0].version).isEqualTo(0)

        helper.uploadNewVersionFromResource(
            "test/bpmn/$procId.bpmn.xml",
            procId,
            comment,
            "some documentation" to "new documentation"
        )

        val newRevisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
        assertThat(newRevisions).hasSize(2)
        assertThat(newRevisions[0].version).isEqualTo(1)
        assertThat(newRevisions[0].comment).isEqualTo(comment)
        assertThat(newRevisions[1].version).isEqualTo(0)
    }

    @ParameterizedTest
    @ValueSource(strings = ["test-new-version", "test-new-version-draft"])
    fun `copied bpmn process, modify and upload as new version to root process`(procId: String) {
        val copiedId = "$procId-copy"

        helper.saveBpmnWithAction("test/bpmn/$procId.bpmn.xml", procId, null)
        helper.copyBpmnModule(procId, copiedId)

        val copiedDef = procDefService.getProcessDefById(ProcDefRef.createWoWs(BPMN_PROC_TYPE, copiedId))
            ?: error("Copied definition not found")

        helper.uploadNewVersion(
            String(copiedDef.data),
            procId,
            "Upload to root process",
            "some documentation" to "new documentation"
        )

        val rootProcessRevisions = procDefService.getProcessDefRevs(ProcDefRef.createWoWs(BPMN_PROC_TYPE, procId))
        assertThat(rootProcessRevisions).hasSize(2)

        val modifiedNewVersionContent = String(rootProcessRevisions[0].loadData(procDefRevDataProvider))
        assertThat(modifiedNewVersionContent).contains("new documentation")

        val procDef = BpmnXmlUtils.readFromString(modifiedNewVersionContent)
        val modifiedNewVersionContentProcDefIdFromXml = procDef.otherAttributes[BPMN_PROP_PROCESS_DEF_ID].toString()
        // We loaded [copiedId] as new version to [procId].
        // [BPMN_PROP_PROCESS_DEF_ID] should be changed to [procId]
        assertThat(modifiedNewVersionContentProcDefIdFromXml).isEqualTo(procId)

        val rootProcessContent = String(rootProcessRevisions[1].loadData(procDefRevDataProvider))
        assertThat(rootProcessContent).contains("some documentation")
    }
}
