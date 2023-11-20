package ru.citeck.ecos.process.domain.bpmn.bpmnreport

import org.apache.commons.lang3.LocaleUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmnreport.model.*
import ru.citeck.ecos.process.domain.bpmnreport.service.ReportElementsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class ReportElementsServiceTest {

    companion object {
        private val TEST_TYPE = EntityRef.create(AppName.EMODEL, "type", "bpmn-report-test-type")
    }

    @Autowired
    lateinit var reportElementsService: ReportElementsService

    @Test
    fun convertReportStatusElementTest() {

        val expectedElement = ReportStatusElement(
            name = MLText(
                LocaleUtils.toLocale("ru") to "Статус - Завершен",
                LocaleUtils.toLocale("en") to "Status - Completed"
            ),
            status = MLText(
                LocaleUtils.toLocale("ru") to "Завершен",
                LocaleUtils.toLocale("en") to "Completed"
            )
        )

        val flowElementDef = BpmnFlowElementDef(
            id = "test_status",
            type = ElementType.STATUS.flowElementType,
            data = ObjectData.create(
                mapOf(
                    "name" to MLText(
                        LocaleUtils.toLocale("ru") to "Статус - Завершен",
                        LocaleUtils.toLocale("en") to "Status - Completed"
                    ),
                    "ecosTaskDefinition" to mapOf(
                        "status" to "completed-test"
                    )
                )
            )
        )

        val actualElement =
            reportElementsService.convertReportStatusElement(flowElementDef, TEST_TYPE)

        assertEquals(expectedElement, actualElement, "Status elements are different!")
    }

    @Test
    fun convertReportGatewayElement() {

        val expectedElement = ReportBaseElement(
            type = ElementType.PARALLEL_GATEWAY.type,
            name = MLText(
                LocaleUtils.toLocale("ru") to "Параллель",
                LocaleUtils.toLocale("en") to "Parallel"
            ),
            documentation = MLText(
                LocaleUtils.toLocale("ru") to "Параллель Документация",
                LocaleUtils.toLocale("en") to "Parallel Documentation"
            )
        )

        val flowElementDef = BpmnFlowElementDef(
            id = "test_parallel",
            type = ElementType.PARALLEL_GATEWAY.flowElementType,
            data = ObjectData.create(
                mapOf(
                    "name" to MLText(
                        LocaleUtils.toLocale("ru") to "Параллель",
                        LocaleUtils.toLocale("en") to "Parallel"
                    ),
                    "documentation" to MLText(
                        LocaleUtils.toLocale("ru") to "Параллель Документация",
                        LocaleUtils.toLocale("en") to "Parallel Documentation"
                    )
                )
            )
        )

        val actualElement =
            reportElementsService.convertReportGatewayElement(flowElementDef, ElementType.PARALLEL_GATEWAY)

        assertEquals(expectedElement, actualElement, "Parallel gateways are different!")
    }

    @Test
    fun convertReportSubProcessElementTest() {

        val expectedElement = ReportSubProcessElement().apply {
            type = ElementType.SUB_PROCESS.type
            name = MLText(
                LocaleUtils.toLocale("ru") to "Тестовый подпроцесс",
                LocaleUtils.toLocale("en") to "Test subprocess"
            )
            documentation = MLText(
                LocaleUtils.toLocale("ru") to "Документация подпроцесса",
                LocaleUtils.toLocale("en") to "Documentation of subprocess"
            )
        }

        val flowElementDef = BpmnFlowElementDef(
            id = "test_subprocess",
            type = ElementType.SUB_PROCESS.flowElementType,
            data = ObjectData.create(
                mapOf(
                    "name" to MLText(
                        LocaleUtils.toLocale("ru") to "Тестовый подпроцесс",
                        LocaleUtils.toLocale("en") to "Test subprocess"
                    ),
                    "documentation" to MLText(
                        LocaleUtils.toLocale("ru") to "Документация подпроцесса",
                        LocaleUtils.toLocale("en") to "Documentation of subprocess"
                    )
                )
            )
        )

        val actualElement =
            reportElementsService.convertReportSubProcessElement(flowElementDef, ElementType.SUB_PROCESS)

        assertEquals(expectedElement, actualElement, "Subprocesses are different!")
    }

    @Test
    fun convertReportEventElementTest() {

        val expectedElement = ReportEventElement().apply {
            type = "Timer ${ElementType.BOUNDARY_EVENT.type} (Non Interrupting)"
            name = MLText(
                LocaleUtils.toLocale("ru") to "Таймер",
                LocaleUtils.toLocale("en") to "Timer"
            )
            documentation = MLText(
                LocaleUtils.toLocale("ru") to "Таймер задачи",
                LocaleUtils.toLocale("en") to "Task Timer"
            )
            eventType = MLText(
                LocaleUtils.toLocale("en") to "Duration",
                LocaleUtils.toLocale("ru") to "Продолжительность"
            )
            value = "PT5S"
        }

        val flowElementDef = BpmnFlowElementDef(
            id = "test_boundary_event",
            type = ElementType.BOUNDARY_EVENT.flowElementType,
            data = ObjectData.create(
                mapOf(
                    "name" to MLText(
                        LocaleUtils.toLocale("ru") to "Таймер",
                        LocaleUtils.toLocale("en") to "Timer"
                    ),
                    "documentation" to MLText(
                        LocaleUtils.toLocale("ru") to "Таймер задачи",
                        LocaleUtils.toLocale("en") to "Task Timer"
                    ),
                    "cancelActivity" to false,
                    "eventDefinition" to mapOf(
                        "id" to "test_event",
                        "type" to "timerEvent",
                        "value" to mapOf(
                            "type" to "DURATION",
                            "value" to "PT5S"
                        )
                    )
                )
            )
        )

        val actualElement =
            reportElementsService.convertReportEventElement(flowElementDef, ElementType.BOUNDARY_EVENT)

        assertEquals(expectedElement, actualElement, "Events are different!")
    }

    @Test
    fun convertReportTaskElement() {

        val expectedElement = ReportTaskElement().apply {
            type = ElementType.USER_TASK.type
            name = MLText(
                LocaleUtils.toLocale("ru") to "Проверка",
                LocaleUtils.toLocale("en") to "Check"
            )
            documentation = MLText(
                LocaleUtils.toLocale("ru") to "Проверка бухгалтером",
                LocaleUtils.toLocale("en") to "Accountant check"
            )
            outcomes = ArrayList(
                listOf(
                    ReportUserTaskOutcomeElement(
                        MLText(
                            LocaleUtils.toLocale("ru") to "Одобрено",
                            LocaleUtils.toLocale("en") to "Approved"
                        )
                    ),
                    ReportUserTaskOutcomeElement(
                        MLText(
                            LocaleUtils.toLocale("ru") to "Отклонено",
                            LocaleUtils.toLocale("en") to "Rejected"
                        )
                    )
                )
            )
            assignees = ReportUserTaskAssigneeElement(
                roles = ArrayList(
                    listOf(
                        ReportRoleElement(
                            name = MLText(
                                LocaleUtils.toLocale("ru") to "Бухгалтер",
                                LocaleUtils.toLocale("en") to "Accountant"
                            )
                        )
                    )
                )
            )
        }

        val flowElementDef = BpmnFlowElementDef(
            id = "test_user_task",
            type = ElementType.USER_TASK.flowElementType,
            data = ObjectData.create(
                mapOf(
                    "name" to MLText(
                        LocaleUtils.toLocale("ru") to "Проверка",
                        LocaleUtils.toLocale("en") to "Check"
                    ),
                    "documentation" to MLText(
                        LocaleUtils.toLocale("ru") to "Проверка бухгалтером",
                        LocaleUtils.toLocale("en") to "Accountant check"
                    ),
                    "outcomes" to listOf(
                        mapOf(
                            "name" to MLText(
                                LocaleUtils.toLocale("ru") to "Одобрено",
                                LocaleUtils.toLocale("en") to "Approved"
                            )
                        ),
                        mapOf(
                            "name" to MLText(
                                LocaleUtils.toLocale("ru") to "Отклонено",
                                LocaleUtils.toLocale("en") to "Rejected"
                            )
                        )
                    ),
                    "assignees" to listOf(
                        mapOf(
                            "value" to "accountant-test"
                        )
                    )
                )
            )
        )

        val actualElement =
            reportElementsService.convertReportTaskElement(flowElementDef, ElementType.USER_TASK, TEST_TYPE)

        assertEquals(expectedElement, actualElement, "Tasks are different!")
    }
}
