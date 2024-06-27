package ru.citeck.ecos.process.domain

import org.apache.commons.lang3.LocaleUtils
import org.assertj.core.api.Assertions
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
import ru.citeck.ecos.model.lib.attributes.dto.AttributeType
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.BPMN_FORMAT
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.kpi.BPMN_KPI_SETTINGS_SOURCE_ID
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnDurationKpiTimeType
import ru.citeck.ecos.process.domain.bpmn.kpi.BpmnKpiEventType
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiSettings
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessService
import ru.citeck.ecos.process.domain.dmn.DMN_FORMAT
import ru.citeck.ecos.process.domain.dmn.DMN_PROC_TYPE
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSettingsMeta
import ru.citeck.ecos.process.domain.proctask.attssync.TaskAttsSyncSource
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttribute
import ru.citeck.ecos.process.domain.proctask.attssync.TaskSyncAttributeType
import ru.citeck.ecos.process.domain.proctask.config.PROC_TASK_ATTS_SYNC_SOURCE_ID
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val DMN_TEST_DEF_ID = "dmn-test-artifact-handler"
private const val DMN_TEST_DECISION_ID = "decision-test-deploy"

private const val BPMN_TEST_PROCESS_ID = "bpmn-test-process"
private const val BPMN_TEST_DRAFT_PROCESS_ID = "test-draft-process-artifact-handler"

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArtifactsHandlerTest {

    @Autowired
    private lateinit var localAppService: LocalAppService

    @Autowired
    private lateinit var camundaRepoService: RepositoryService

    @Autowired
    private lateinit var procDefService: ProcDefService

    @Autowired
    private lateinit var bpmnProcessService: BpmnProcessService

    @Autowired
    private lateinit var recordsService: RecordsService

    companion object {
        private val durationSettingsRef = EntityRef.create(
            AppName.EMODEL,
            BPMN_KPI_SETTINGS_SOURCE_ID,
            "test-duration-kpi-settings"
        )
        private val countSettingsReF = EntityRef.create(
            AppName.EMODEL,
            BPMN_KPI_SETTINGS_SOURCE_ID,
            "test-count-kpi-settings"
        )
    }

    @BeforeAll
    fun setUp() {
        AuthContext.runAsSystem {
            localAppService.deployLocalArtifacts()
        }
    }

    @Test
    fun `check definition meta data of loaded dmn from artifact`() {
        val deployed = procDefService.getProcessDefById(
            ProcDefRef.create(
                DMN_PROC_TYPE,
                DMN_TEST_DEF_ID
            )
        )

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
        assertEquals(DMN_TEST_DEF_ID + DMN_RESOURCE_NAME_POSTFIX, dmnDecisions.first().resourceName)
        assertEquals(1, dmnDecisions.first().version)
    }

    @Test
    fun `check definition meta data of loaded bpmn process from artifact`() {
        val definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_PROCESS_ID))

        assertNotNull(definition)

        assertEquals(BPMN_TEST_PROCESS_ID, definition.id)
        assertEquals(MLText(mapOf(LocaleUtils.toLocale("ru") to "Test process")), definition.name)

        assertEquals(EntityRef.valueOf("uiserv/form@test-bpmn-form"), definition.formRef)
        assertEquals(EntityRef.valueOf("emodel/type@type-ecos-fin-request"), definition.ecosTypeRef)
        assertEquals(BPMN_FORMAT, definition.format)
        assertEquals(BPMN_PROC_TYPE, definition.procType)

        assertNull(definition.alfType)

        assertTrue(definition.enabled)
        assertTrue(definition.autoStartEnabled)

        assertTrue(definition.data != null && definition.data.isNotEmpty())
    }

    @Test
    fun `loaded bpmn process from artifact should save revision with converted data state`() {
        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_PROCESS_ID))

        assertEquals(1, revisions.size)

        val revision = revisions.first()

        assertEquals(ProcDefRevDataState.CONVERTED, revision.dataState)
    }

    @Test
    fun `loaded bpmn process from artifact should save revision with deployment id`() {
        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_PROCESS_ID))

        val revision = revisions.first()

        assertNotNull(revision.deploymentId)
        Assertions.assertThat(revision.deploymentId).isNotBlank()
    }

    @Test
    fun `loaded bpmn process from artifact should be deployed to engine`() {
        val definitions = bpmnProcessService.getProcessDefinitionsByKey(BPMN_TEST_PROCESS_ID)

        assertEquals(1, definitions.size)
        assertEquals(BPMN_TEST_PROCESS_ID, definitions.first().key)
        assertEquals(1, definitions.first().version)
    }

    @Test
    fun `check definition meta data of loaded draft bpmn process from artifact`() {
        val definition = procDefService.getProcessDefById(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_DRAFT_PROCESS_ID))

        assertNotNull(definition)

        assertEquals(BPMN_TEST_DRAFT_PROCESS_ID, definition.id)
        assertEquals(MLText(mapOf(LocaleUtils.toLocale("ru") to "Test draft process")), definition.name)

        assertEquals(EntityRef.valueOf("uiserv/form@ecos-test-form"), definition.formRef)
        assertEquals(EntityRef.valueOf("emodel/type@test-type"), definition.ecosTypeRef)

        assertEquals(BPMN_FORMAT, definition.format)
        assertEquals(BPMN_PROC_TYPE, definition.procType)

        assertNull(definition.alfType)

        assertTrue(definition.enabled)

        assertTrue(definition.data != null && definition.data.isNotEmpty())
    }

    @Test
    fun `loaded draft bpmn process from artifact should save revision with raw data state`() {
        val revisions = procDefService.getProcessDefRevs(ProcDefRef.create(BPMN_PROC_TYPE, BPMN_TEST_DRAFT_PROCESS_ID))

        assertEquals(1, revisions.size)

        val revision = revisions.first()

        assertEquals(ProcDefRevDataState.RAW, revision.dataState)
    }

    @Test
    fun `check bpmn kpi duration settings meta data after deploy`() {
        val durationSettings = recordsService.getAtts(durationSettingsRef, BpmnKpiSettings::class.java)

        Assertions.assertThat(durationSettings.id).isEqualTo("test-duration-kpi-settings")
        Assertions.assertThat(durationSettings.name).isEqualTo("test duration kpi")
        Assertions.assertThat(durationSettings.enabled).isTrue()
        Assertions.assertThat(durationSettings.processRef.toString()).isEqualTo("eproc/bpmn-proc-latest@some-process")
        Assertions.assertThat(durationSettings.dmnCondition.toString())
            .isEqualTo("eproc/dmn-decision-latest@Decision_test")
        Assertions.assertThat(durationSettings.sourceBpmnActivityId).isEqualTo("source_id")
        Assertions.assertThat(durationSettings.sourceBpmnActivityEvent).isEqualTo(BpmnKpiEventType.START)
        Assertions.assertThat(durationSettings.targetBpmnActivityId).isEqualTo("target_id")
        Assertions.assertThat(durationSettings.targetBpmnActivityEvent).isEqualTo(BpmnKpiEventType.END)
        Assertions.assertThat(durationSettings.durationKpi).isEqualTo("1h 30m")
        Assertions.assertThat(durationSettings.durationKpiTimeType).isEqualTo(BpmnDurationKpiTimeType.WORKING)
    }

    @Test
    fun `check bpmn kpi count settings meta data after deploy`() {
        val countSettings = recordsService.getAtts(countSettingsReF, BpmnKpiSettings::class.java)

        Assertions.assertThat(countSettings.id).isEqualTo("test-count-kpi-settings")
        Assertions.assertThat(countSettings.name).isEqualTo("test count kpi")
        Assertions.assertThat(countSettings.enabled).isTrue()
        Assertions.assertThat(countSettings.processRef.toString()).isEqualTo("eproc/bpmn-proc-latest@some-process")
        Assertions.assertThat(countSettings.dmnCondition.toString())
            .isEqualTo("eproc/dmn-decision-latest@Decision_test")
        Assertions.assertThat(countSettings.targetBpmnActivityId).isEqualTo("target_id")
        Assertions.assertThat(countSettings.targetBpmnActivityEvent).isEqualTo(BpmnKpiEventType.START)
        Assertions.assertThat(countSettings.countKpi).isEqualTo(30)
    }

    @Test
    fun `check task record atts sync meta data after deploy`() {
        val ref = EntityRef.create(
            AppName.EPROC,
            PROC_TASK_ATTS_SYNC_SOURCE_ID,
            "test-task-atts-sync-record-artifact"
        )
        val taskAttsSync = recordsService.getAtts(
            ref,
            TaskAttsSyncSettingsMeta::class.java
        )

        Assertions.assertThat(taskAttsSync.id).isEqualTo(ref)
        Assertions.assertThat(taskAttsSync.name).isEqualTo("test task atts sync record artifact")
        Assertions.assertThat(taskAttsSync.enabled).isTrue()
        Assertions.assertThat(taskAttsSync.source).isEqualTo(TaskAttsSyncSource.RECORD)
        Assertions.assertThat(taskAttsSync.attributesSync).containsExactlyInAnyOrder(
            TaskSyncAttribute(
                id = "name",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@doc"),
                        attribute = "name"
                    )
                )
            ),
            TaskSyncAttribute(
                id = "sum",
                type = AttributeType.NUMBER,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@doc"),
                        attribute = "sum"
                    )
                )
            ),
            TaskSyncAttribute(
                id = "count",
                type = AttributeType.NUMBER,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@doc"),
                        attribute = "count"
                    )
                )
            )
        )
    }

    @Test
    fun `check task type atts sync meta data after deploy`() {
        val ref = EntityRef.create(
            AppName.EPROC,
            PROC_TASK_ATTS_SYNC_SOURCE_ID,
            "test-task-atts-sync-type-artifact"
        )
        val taskAttsSync = recordsService.getAtts(
            ref,
            TaskAttsSyncSettingsMeta::class.java
        )

        Assertions.assertThat(taskAttsSync.id).isEqualTo(ref)
        Assertions.assertThat(taskAttsSync.name).isEqualTo("test task atts sync type artifact")
        Assertions.assertThat(taskAttsSync.enabled).isTrue()
        Assertions.assertThat(taskAttsSync.source).isEqualTo(TaskAttsSyncSource.TYPE)
        Assertions.assertThat(taskAttsSync.attributesSync).containsExactlyInAnyOrder(
            TaskSyncAttribute(
                id = "procedure",
                type = AttributeType.TEXT,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@doc"),
                        recordExpressionAttribute = "config.procedure"
                    )
                )
            ),
            TaskSyncAttribute(
                id = "urgency",
                type = AttributeType.NUMBER,
                ecosTypes = listOf(
                    TaskSyncAttributeType(
                        typeRef = EntityRef.valueOf("emodel/type@doc"),
                        recordExpressionAttribute = "config.urgency?num"
                    )
                )
            )
        )
    }
}
