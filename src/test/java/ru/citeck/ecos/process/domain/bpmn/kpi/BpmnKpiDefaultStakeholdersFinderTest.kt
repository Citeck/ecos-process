package ru.citeck.ecos.process.domain.bpmn.kpi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.kpi.stakeholders.BpmnKpiDefaultStakeholdersFinder
import ru.citeck.ecos.process.domain.createDurationKpiSettings
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BpmnKpiDefaultStakeholdersFinderTest {

    @Autowired
    private lateinit var bpmnKpiDefaultStakeholdersFinder: BpmnKpiDefaultStakeholdersFinder

    @Autowired
    private lateinit var recordsService: RecordsService

    private val kpiSettings = mutableListOf<EntityRef>()

    companion object {
        private const val TEST_PROCESS = "test-process"
    }

    @BeforeAll
    fun setUp() {
        kpiSettings.add(
            createDurationKpiSettings(
                id = "settings_duration_start_end_0",
                process = TEST_PROCESS,
                source = "activitySource",
                sourceEventType = BpmnKpiEventType.START,
                target = "activityTarget",
                targetEventType = BpmnKpiEventType.END,
                kpiType = BpmnKpiType.DURATION
            )
        )
        kpiSettings.add(
            createDurationKpiSettings(
                id = "settings_duration_start_end_1",
                process = TEST_PROCESS,
                source = "activitySource1",
                sourceEventType = BpmnKpiEventType.START,
                target = "activityTarget1",
                targetEventType = BpmnKpiEventType.END,
                kpiType = BpmnKpiType.DURATION
            )
        )
        kpiSettings.add(
            createDurationKpiSettings(
                id = "settings_duration_start_start_0",
                process = TEST_PROCESS,
                source = "activitySource",
                sourceEventType = BpmnKpiEventType.START,
                target = "activityTarget",
                targetEventType = BpmnKpiEventType.START,
                kpiType = BpmnKpiType.DURATION
            )
        )
        kpiSettings.add(
            createDurationKpiSettings(
                id = "settings_count_start",
                process = TEST_PROCESS,
                target = "activityTarget",
                targetEventType = BpmnKpiEventType.START,
                kpiType = BpmnKpiType.COUNT
            )
        )
        kpiSettings.add(
            createDurationKpiSettings(
                id = "settings_count_end",
                process = TEST_PROCESS,
                target = "activityTarget",
                targetEventType = BpmnKpiEventType.END,
                kpiType = BpmnKpiType.COUNT
            )
        )
    }

    @Test
    fun `search kpi settings trigger source start`() {
        val stakeholders = bpmnKpiDefaultStakeholdersFinder.searchStakeholders(
            processId = TEST_PROCESS,
            activityId = "activitySource",
            eventType = BpmnKpiEventType.START,
            BpmnKpiType.DURATION
        )

        assertThat(stakeholders).isEmpty()
    }

    @Test
    fun `search kpi settings trigger source end`() {
        val stakeholders = bpmnKpiDefaultStakeholdersFinder.searchStakeholders(
            processId = TEST_PROCESS,
            activityId = "activitySource",
            eventType = BpmnKpiEventType.END,
            BpmnKpiType.DURATION
        )

        assertThat(stakeholders).isEmpty()
    }

    @Test
    fun `search kpi settings trigger target start duration`() {
        val stakeholders = bpmnKpiDefaultStakeholdersFinder.searchStakeholders(
            processId = TEST_PROCESS,
            activityId = "activityTarget",
            eventType = BpmnKpiEventType.START,
            BpmnKpiType.DURATION
        )

        assertThat(stakeholders).hasSize(1)
        assertThat(stakeholders.map { it.getRef().getLocalId() }).hasSameElementsAs(
            listOf(
                "settings_duration_start_start_0"
            )
        )
    }

    @Test
    fun `search kpi settings trigger target start count`() {
        val stakeholders = bpmnKpiDefaultStakeholdersFinder.searchStakeholders(
            processId = TEST_PROCESS,
            activityId = "activityTarget",
            eventType = BpmnKpiEventType.START,
            BpmnKpiType.COUNT
        )

        assertThat(stakeholders).hasSize(1)
        assertThat(stakeholders.map { it.getRef().getLocalId() }).hasSameElementsAs(
            listOf(
                "settings_count_start"
            )
        )
    }

    @Test
    fun `search kpi settings trigger target end duration`() {
        val stakeholders = bpmnKpiDefaultStakeholdersFinder.searchStakeholders(
            processId = TEST_PROCESS,
            activityId = "activityTarget",
            eventType = BpmnKpiEventType.END,
            BpmnKpiType.DURATION
        )

        assertThat(stakeholders).hasSize(1)
        assertThat(stakeholders.map { it.getRef().getLocalId() }).hasSameElementsAs(
            listOf(
                "settings_duration_start_end_0"
            )
        )
    }

    @Test
    fun `search kpi settings trigger target end count`() {
        val stakeholders = bpmnKpiDefaultStakeholdersFinder.searchStakeholders(
            processId = TEST_PROCESS,
            activityId = "activityTarget",
            eventType = BpmnKpiEventType.END,
            BpmnKpiType.COUNT
        )

        assertThat(stakeholders).hasSize(1)
        assertThat(stakeholders.map { it.getRef().getLocalId() }).hasSameElementsAs(
            listOf(
                "settings_count_end"
            )
        )
    }

    @AfterAll
    fun tearDown() {
        recordsService.delete(kpiSettings)
    }
}
