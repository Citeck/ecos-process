package ru.citeck.ecos.process.domain.bpmn.bpmnreport

import org.apache.commons.lang3.LocaleUtils
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmnreport.model.*
import ru.citeck.ecos.process.domain.bpmnreport.service.BpmnProcessReportService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnProcessReportServiceTest {

    @Autowired
    lateinit var processReportService: BpmnProcessReportService

    @Autowired
    lateinit var bpmnIO: BpmnIO

    @Test
    fun generateReportElementListForBpmnDefinitionTest() {

        val mainReportProcessElement = ReportProcessElement(
            id = "bpmn-report-test-process",
            participant = ReportParticipantElement(
                number = "1",
                name = MLText(
                    LocaleUtils.toLocale("ru") to "Основной процесс",
                    LocaleUtils.toLocale("en") to "Main process"
                ),
                documentation = MLText(
                    LocaleUtils.toLocale("ru") to "Документация основного процесса",
                    LocaleUtils.toLocale("en") to "Documentation of main process"
                )
            )
        )

        val secondProcessReportProcessElement = ReportProcessElement(
            id = "bpmn-report-test-process-2",
            participant = ReportParticipantElement(
                number = "2",
                name = MLText(
                    LocaleUtils.toLocale("ru") to "Процесс 2",
                    LocaleUtils.toLocale("en") to "Process 2"
                )
            )
        )

        val mainLane1ReportLaneElement = ReportLaneElement(
            number = 1,
            name = MLText(
                LocaleUtils.toLocale("ru") to "Инициатор",
                LocaleUtils.toLocale("en") to "Initiator"
            ),
            documentation = MLText(
                LocaleUtils.toLocale("ru") to "Действия инициатора",
                LocaleUtils.toLocale("en") to "Initiator actions"
            )
        )

        val mainLane2ReportLaneElement = ReportLaneElement(
            number = 2,
            name = MLText(
                LocaleUtils.toLocale("ru") to "Система",
                LocaleUtils.toLocale("en") to "System"
            ),
            documentation = MLText(
                LocaleUtils.toLocale("ru") to "Действия системы",
                LocaleUtils.toLocale("en") to "System actions"
            )
        )

        val expectedList = listOf(
            ReportElement(
                id = "StartEvent_1ew9rff",
                prefix = null,
                number = "1",
                process = mainReportProcessElement,
                lane = mainLane1ReportLaneElement,
                eventElement = ReportEventElement(
                    type = ElementType.START_EVENT.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Создание карточки",
                        LocaleUtils.toLocale("en") to "Create card"
                    )
                )
            ),
            ReportElement(
                id = "Activity_1kl13e5",
                prefix = null,
                number = "2",
                process = mainReportProcessElement,
                lane = mainLane1ReportLaneElement,
                taskElement = ReportTaskElement(
                    type = ElementType.USER_TASK.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Задача согласования",
                        LocaleUtils.toLocale("en") to "Approval task"
                    ),
                    documentation = MLText(
                        LocaleUtils.toLocale("ru") to "Документация по задаче согласования",
                        LocaleUtils.toLocale("en") to "Documentation of approval task"
                    ),
                    outcomes = ArrayList(
                        listOf(
                            ReportUserTaskOutcomeElement(
                                name = MLText(
                                    LocaleUtils.toLocale("ru") to "Одобрено",
                                    LocaleUtils.toLocale("en") to "Approved"
                                )
                            ),
                            ReportUserTaskOutcomeElement(
                                name = MLText(
                                    LocaleUtils.toLocale("ru") to "Отклонено",
                                    LocaleUtils.toLocale("en") to "Rejected"
                                )
                            )
                        )
                    ),
                    assignees = ReportUserTaskAssigneeElement(
                        roles = ArrayList(
                            listOf(
                                ReportRoleElement(
                                    name = MLText(
                                        LocaleUtils.toLocale("ru") to "Инициатор",
                                        LocaleUtils.toLocale("en") to "Initiator"
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            ReportElement(
                id = "Gateway_1ffgbao",
                prefix = null,
                number = "3",
                process = mainReportProcessElement,
                lane = mainLane1ReportLaneElement,
                gatewayElement = ReportBaseElement(
                    type = ElementType.EXCLUSIVE_GATEWAY.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Шлюз 1",
                        LocaleUtils.toLocale("en") to "Gateway 1"
                    )
                )
            ),
            ReportElement(
                id = "Activity_11v8b2q",
                prefix = null,
                number = "4",
                process = mainReportProcessElement,
                lane = mainLane1ReportLaneElement,
                statusElement = ReportStatusElement(
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Статус - завершен",
                        LocaleUtils.toLocale("en") to "Status - Approved"
                    ),
                    status = MLText(
                        LocaleUtils.toLocale("ru") to "Завершен",
                        LocaleUtils.toLocale("en") to "Completed"
                    )
                ),
                incoming = listOf(
                    ReportSequenceElement(
                        name = MLText(
                            LocaleUtils.toLocale("ru") to "Карточка одобрена",
                            LocaleUtils.toLocale("en") to "Card approved"
                        ),
                        outcome = MLText(
                            LocaleUtils.toLocale("ru") to "Задача согласования - Одобрено",
                            LocaleUtils.toLocale("en") to "Approval task - Approved"
                        ),
                        type = MLText(
                            LocaleUtils.toLocale("ru") to "Исходящий",
                            LocaleUtils.toLocale("en") to "Outcome"
                        )
                    )
                )
            ),
            ReportElement(
                id = "Event_0g3q0t9",
                prefix = null,
                number = "5",
                process = mainReportProcessElement,
                lane = mainLane1ReportLaneElement,
                eventElement = ReportEventElement(
                    type = ElementType.END_EVENT.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Карточка одобрена",
                        LocaleUtils.toLocale("en") to "Approved card"
                    )
                )
            ),
            ReportElement(
                id = "Event_1c8v8jj",
                prefix = null,
                number = "6",
                process = mainReportProcessElement,
                lane = mainLane1ReportLaneElement,
                eventElement = ReportEventElement(
                    type = "Timer Boundary Event (Non Interrupting)",
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Проверка времени",
                        LocaleUtils.toLocale("en") to "Check time"
                    ),
                    eventType = MLText(
                        LocaleUtils.toLocale("ru") to "Продолжительность",
                        LocaleUtils.toLocale("en") to "Duration"
                    ),
                    value = "P14D"
                )
            ),
            ReportElement(
                id = "Activity_14dolni",
                prefix = null,
                number = "7",
                process = mainReportProcessElement,
                lane = mainLane2ReportLaneElement,
                taskElement = ReportTaskElement(
                    type = ElementType.SEND_TASK.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Уведомление о просрочке задачи",
                        LocaleUtils.toLocale("en") to "Notification of task overdue"
                    ),
                    recipients = ReportSendTaskRecipientsElement(
                        to = ReportSendTaskRecipientElement(
                            roles = ArrayList(
                                listOf(
                                    ReportRoleElement(
                                        name = MLText(
                                            LocaleUtils.toLocale("ru") to "Инициатор",
                                            LocaleUtils.toLocale("en") to "Initiator"
                                        )
                                    )
                                )
                            )
                        ),
                        cc = ReportSendTaskRecipientElement(
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
                    )
                )
            ),
            ReportElement(
                id = "Activity_0hro9d5",
                prefix = null,
                number = "8",
                process = mainReportProcessElement,
                lane = mainLane2ReportLaneElement,
                subProcessElement = ReportSubProcessElement(
                    type = ElementType.CALL_ACTIVITY.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Вызов процесса 2",
                        LocaleUtils.toLocale("en") to "Activate process 2"
                    ),
                    subProcessName = "bpmn-report-test-process-2"
                ),
                incoming = listOf(
                    ReportSequenceElement(
                        name = MLText(
                            LocaleUtils.toLocale("ru") to "Карточка отклонена",
                            LocaleUtils.toLocale("en") to "Card Rejected"
                        ),
                        outcome = MLText(
                            LocaleUtils.toLocale("ru") to "Задача согласования - Отклонено",
                            LocaleUtils.toLocale("en") to "Approval task - Rejected"
                        ),
                        type = MLText(
                            LocaleUtils.toLocale("ru") to "Исходящий",
                            LocaleUtils.toLocale("en") to "Outcome"
                        )
                    )
                ),
                annotations = ArrayList(
                    listOf(
                        ReportAnnotationElement(
                            annotation = MLText(
                                LocaleUtils.toLocale("ru") to "Запускаем 2 процесс",
                                LocaleUtils.toLocale("en") to "Start process 2"
                            )
                        )
                    )
                )
            ),
            ReportElement(
                id = "Event_1108d97",
                prefix = null,
                number = "1",
                process = secondProcessReportProcessElement,
                eventElement = ReportEventElement(
                    type = ElementType.START_EVENT.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Старт 2 процесса",
                        LocaleUtils.toLocale("en") to "Start process 2"
                    )
                )
            ),
            ReportElement(
                id = "Activity_1m2gh5p",
                prefix = null,
                number = "2",
                process = secondProcessReportProcessElement,
                taskElement = ReportTaskElement(
                    type = ElementType.SCRIPT_TASK.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Скрипт задача",
                        LocaleUtils.toLocale("en") to "Script task"
                    )
                ),
                annotations = ArrayList(
                    listOf(
                        ReportAnnotationElement(
                            annotation = MLText(
                                LocaleUtils.toLocale("ru") to "Простая аннотация 2",
                                LocaleUtils.toLocale("en") to "Just annotation 2"
                            )
                        )
                    )
                )
            ),
            ReportElement(
                id = "Event_0x2fpow",
                prefix = null,
                number = "3",
                process = secondProcessReportProcessElement,
                eventElement = ReportEventElement(
                    type = ElementType.END_EVENT.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Конец 2 процесса",
                        LocaleUtils.toLocale("en") to "End process 2"
                    )
                )
            ),
            ReportElement(
                id = "Activity_1ljphcd",
                prefix = null,
                number = "sub1",
                process = secondProcessReportProcessElement,
                subProcessElement = ReportSubProcessElement(
                    type = ElementType.SUB_PROCESS.type,
                    elements = listOf("Event_0lc5q55", "Event_0gvcqnd"),
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Подпроцесс"
                    )
                )
            ),
            ReportElement(
                id = "Event_0lc5q55",
                prefix = "sub1-",
                number = "1",
                process = secondProcessReportProcessElement,
                eventElement = ReportEventElement(
                    type = ElementType.START_EVENT.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Под-старт"
                    )
                ),
                subProcessElement = ReportSubProcessElement(
                    type = ElementType.SUB_PROCESS.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Подпроцесс"
                    )
                )
            ),
            ReportElement(
                id = "Event_0gvcqnd",
                prefix = "sub1-",
                number = "2",
                process = secondProcessReportProcessElement,
                eventElement = ReportEventElement(
                    type = ElementType.END_EVENT.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Под-конец"
                    )
                ),
                subProcessElement = ReportSubProcessElement(
                    type = ElementType.SUB_PROCESS.type,
                    name = MLText(
                        LocaleUtils.toLocale("ru") to "Подпроцесс"
                    )
                )
            )
        )

        val fileDefinition = ResourceUtils.getFile(
            "classpath:test/bpmn/report/bpmn-report-test-process.bpmn.xml"
        ).readText(StandardCharsets.UTF_8)
        val bpmnDefinitionDef = bpmnIO.importEcosBpmn(fileDefinition, validate = false)

        val actualList = processReportService.generateReportElementListForBpmnDefinition(bpmnDefinitionDef)

        assertEquals(expectedList.size, actualList.size, "Number of elements is different!")
        assertEquals(expectedList, actualList, "Elements are different!")
    }
}
