//package ru.citeck.ecos.process.domain.bpmn
//
//import com.github.javafaker.Faker
//import org.camunda.bpm.engine.ProcessEngine
//import org.camunda.bpm.engine.test.assertions.ProcessEngineTests.assertThat
//import org.camunda.bpm.scenario.ProcessScenario
//import org.camunda.bpm.scenario.Scenario
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.extension.ExtendWith
//import org.mockito.ArgumentMatchers.anyString
//import org.mockito.Mock
//import org.mockito.Mockito.`when`
//import org.mockito.kotlin.verify
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.test.mock.mockito.SpyBean
//import ru.citeck.ecos.process.EprocApp
//import ru.citeck.ecos.process.domain.BpmnProcHelper
//import ru.citeck.ecos.process.domain.bpmn.engine.camunda.BPMN_DOCUMENT_REF
//import ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans.CamundaRoleService
//import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
//
//@ExtendWith(EcosSpringExtension::class)
//@SpringBootTest(classes = [EprocApp::class])
//class BpmnUserTaskLargeAssigneeTest {
//
//    companion object {
//        private val faker = Faker.instance()
//
//        private val mockRoleUserNames = (1..100).map { faker.name().username() }
//        private val mockGroups = (1..100).map { "GROUP_" + faker.pokemon().name() }
//    }
//
//    @Mock
//    private lateinit var process: ProcessScenario
//
//    @Autowired
//    private lateinit var processEngine: ProcessEngine
//
//    @SpyBean
//    private lateinit var camundaRoleService: CamundaRoleService
//
//    @Autowired
//    private lateinit var helper: BpmnProcHelper
//
//    @BeforeEach
//    fun setUo() {
//        `when`(camundaRoleService.getUserNames(anyString(), anyString())).thenReturn(mockRoleUserNames)
//        `when`(camundaRoleService.getGroupNames(anyString(), anyString())).thenReturn(mockGroups)
//    }
//
//    @Test
//    fun `complete task with large assignee`() {
//
//        val procId = "test-user-task-large-assignee"
//        helper.saveAndDeployBpmn("usertask", procId)
//
//        `when`(process.waitsAtUserTask("Task_large_assignee")).thenReturn { task ->
//
//            mockRoleUserNames.forEach { userName ->
//                assertThat(task).hasCandidateUser(userName)
//            }
//
//            mockGroups.forEach { groupName ->
//                assertThat(task).hasCandidateGroup(groupName)
//            }
//
//            task.complete()
//        }
//
//        Scenario.run(process).startByKey(
//            procId,
//            mapOf(
//                BPMN_DOCUMENT_REF to "somedoc"
//            )
//        ).engine(processEngine).execute()
//
//        verify(process).hasFinished("Event_end2")
//    }
//
//
//}
