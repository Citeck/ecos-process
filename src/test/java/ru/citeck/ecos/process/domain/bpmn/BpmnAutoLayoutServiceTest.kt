package ru.citeck.ecos.process.domain.bpmn

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutService
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension

@SpringBootTest(classes = [EprocApp::class])
@ExtendWith(EcosSpringExtension::class)
class BpmnAutoLayoutServiceTest {

    @Autowired
    lateinit var bpmnAutoLayoutService: BpmnAutoLayoutService

    @Test
    fun `test service initialization`() {
        val isReady = bpmnAutoLayoutService.isReady()

        assertTrue(isReady, "BpmnAutoLayoutService should be initialized and ready")
    }

    @Test
    fun `test apply auto-layout to simple BPMN`() {
        val simpleBpmn = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
                    xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
                    xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
                    id="Definitions_1"
                    targetNamespace="http://bpmn.io/schema/bpmn">
              <bpmn:process id="Process_1" isExecutable="true">
                <bpmn:startEvent id="StartEvent_1" />
                <bpmn:task id="Task_1" name="Sample Task">
                  <bpmn:incoming>Flow_1</bpmn:incoming>
                  <bpmn:outgoing>Flow_2</bpmn:outgoing>
                </bpmn:task>
                <bpmn:endEvent id="EndEvent_1">
                  <bpmn:incoming>Flow_2</bpmn:incoming>
                </bpmn:endEvent>
                <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1" />
                <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1" />
              </bpmn:process>
            </bpmn:definitions>
        """.trimIndent()

        val result = bpmnAutoLayoutService.applyAutoLayout(simpleBpmn)

        assertNotNull(result, "Result should not be null")
        assertTrue(result.contains("BPMNDiagram"), "Result should contain BPMNDiagram")
        assertTrue(result.contains("BPMNPlane"), "Result should contain BPMNPlane")
        assertTrue(result.contains("BPMNShape"), "Result should contain BPMNShape elements")
        assertTrue(result.contains("BPMNEdge"), "Result should contain BPMNEdge elements")
        assertTrue(result.contains("dc:Bounds"), "Result should contain Bounds for shapes")
        assertTrue(result.contains("di:waypoint"), "Result should contain waypoints for edges")
    }

    @Test
    fun `test apply auto-layout with gateway`() {
        val bpmnWithGateway = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    id="Definitions_1"
                    targetNamespace="http://bpmn.io/schema/bpmn">
              <bpmn:process id="Process_1" isExecutable="true">
                <bpmn:startEvent id="StartEvent_1" />
                <bpmn:exclusiveGateway id="Gateway_1">
                  <bpmn:incoming>Flow_1</bpmn:incoming>
                  <bpmn:outgoing>Flow_2</bpmn:outgoing>
                  <bpmn:outgoing>Flow_3</bpmn:outgoing>
                </bpmn:exclusiveGateway>
                <bpmn:task id="Task_1" name="Task A">
                  <bpmn:incoming>Flow_2</bpmn:incoming>
                  <bpmn:outgoing>Flow_4</bpmn:outgoing>
                </bpmn:task>
                <bpmn:task id="Task_2" name="Task B">
                  <bpmn:incoming>Flow_3</bpmn:incoming>
                  <bpmn:outgoing>Flow_5</bpmn:outgoing>
                </bpmn:task>
                <bpmn:exclusiveGateway id="Gateway_2">
                  <bpmn:incoming>Flow_4</bpmn:incoming>
                  <bpmn:incoming>Flow_5</bpmn:incoming>
                  <bpmn:outgoing>Flow_6</bpmn:outgoing>
                </bpmn:exclusiveGateway>
                <bpmn:endEvent id="EndEvent_1">
                  <bpmn:incoming>Flow_6</bpmn:incoming>
                </bpmn:endEvent>
                <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Gateway_1" />
                <bpmn:sequenceFlow id="Flow_2" sourceRef="Gateway_1" targetRef="Task_1" />
                <bpmn:sequenceFlow id="Flow_3" sourceRef="Gateway_1" targetRef="Task_2" />
                <bpmn:sequenceFlow id="Flow_4" sourceRef="Task_1" targetRef="Gateway_2" />
                <bpmn:sequenceFlow id="Flow_5" sourceRef="Task_2" targetRef="Gateway_2" />
                <bpmn:sequenceFlow id="Flow_6" sourceRef="Gateway_2" targetRef="EndEvent_1" />
              </bpmn:process>
            </bpmn:definitions>
        """.trimIndent()

        val result = bpmnAutoLayoutService.applyAutoLayout(bpmnWithGateway)

        assertNotNull(result, "Result should not be null")
        assertTrue(result.contains("Gateway_1_di"), "Result should contain DI for Gateway_1")
        assertTrue(result.contains("Gateway_2_di"), "Result should contain DI for Gateway_2")
        assertTrue(result.contains("Task_1_di"), "Result should contain DI for Task_1")
        assertTrue(result.contains("Task_2_di"), "Result should contain DI for Task_2")
        assertTrue(result.contains("isMarkerVisible=\"true\""), "Exclusive gateways should have isMarkerVisible attribute")
    }

    @Test
    fun `test apply auto-layout preserves process information`() {
        val bpmnWithAttributes = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
                    id="Definitions_test"
                    targetNamespace="http://example.org/bpmn">
              <bpmn:process id="TestProcess" name="Test Process" isExecutable="true">
                <bpmn:startEvent id="Start" name="Start Event">
                  <bpmn:extensionElements>
                    <camunda:formKey>startForm</camunda:formKey>
                  </bpmn:extensionElements>
                </bpmn:startEvent>
                <bpmn:userTask id="UserTask" name="User Task" camunda:assignee="john">
                  <bpmn:incoming>Flow1</bpmn:incoming>
                </bpmn:userTask>
                <bpmn:sequenceFlow id="Flow1" sourceRef="Start" targetRef="UserTask" />
              </bpmn:process>
            </bpmn:definitions>
        """.trimIndent()

        val result = bpmnAutoLayoutService.applyAutoLayout(bpmnWithAttributes)

        assertNotNull(result, "Result should not be null")

        // Check that process attributes are preserved
        assertTrue(result.contains("id=\"TestProcess\""), "Process ID should be preserved")
        assertTrue(result.contains("name=\"Test Process\""), "Process name should be preserved")

        // Check that element names are preserved
        assertTrue(result.contains("name=\"Start Event\""), "Start event name should be preserved")
        assertTrue(result.contains("name=\"User Task\""), "User task name should be preserved")

        // Check that Camunda extensions are preserved
        assertTrue(result.contains("camunda:assignee=\"john\""), "Camunda assignee should be preserved")
        assertTrue(result.contains("camunda:formKey"), "Camunda formKey should be preserved")

        // Check that diagram information was added
        assertTrue(result.contains("BPMNDiagram"), "Diagram should be added")
        assertTrue(result.contains("BPMNShape"), "Shapes should be added")
    }

    @Test
    fun `test error handling for invalid XML`() {
        val invalidXml = "This is not valid XML"

        assertThrows(IllegalStateException::class.java) {
            bpmnAutoLayoutService.applyAutoLayout(invalidXml)
        }
    }

    @Test
    fun `test subprocess layout`() {
        val bpmnWithSubprocess = """
            <?xml version="1.0" encoding="UTF-8"?>
            <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                    id="Definitions_1">
              <bpmn:process id="Process_1" isExecutable="true">
                <bpmn:startEvent id="StartEvent_1" />
                <bpmn:subProcess id="SubProcess_1">
                  <bpmn:incoming>Flow_1</bpmn:incoming>
                  <bpmn:outgoing>Flow_4</bpmn:outgoing>
                  <bpmn:startEvent id="SubStart_1" />
                  <bpmn:task id="SubTask_1">
                    <bpmn:incoming>SubFlow_1</bpmn:incoming>
                    <bpmn:outgoing>SubFlow_2</bpmn:outgoing>
                  </bpmn:task>
                  <bpmn:endEvent id="SubEnd_1">
                    <bpmn:incoming>SubFlow_2</bpmn:incoming>
                  </bpmn:endEvent>
                  <bpmn:sequenceFlow id="SubFlow_1" sourceRef="SubStart_1" targetRef="SubTask_1" />
                  <bpmn:sequenceFlow id="SubFlow_2" sourceRef="SubTask_1" targetRef="SubEnd_1" />
                </bpmn:subProcess>
                <bpmn:endEvent id="EndEvent_1">
                  <bpmn:incoming>Flow_4</bpmn:incoming>
                </bpmn:endEvent>
                <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="SubProcess_1" />
                <bpmn:sequenceFlow id="Flow_4" sourceRef="SubProcess_1" targetRef="EndEvent_1" />
              </bpmn:process>
            </bpmn:definitions>
        """.trimIndent()

        val result = bpmnAutoLayoutService.applyAutoLayout(bpmnWithSubprocess)

        assertNotNull(result, "Result should not be null")

        // Check main process diagram
        assertTrue(result.contains("BPMNDiagram_Process_1"), "Should have diagram for main process")
        assertTrue(result.contains("SubProcess_1_di"), "SubProcess should have shape")

        // Check subprocess diagram
        assertTrue(result.contains("BPMNDiagram_SubProcess_1"), "Should have separate diagram for subprocess")
        assertTrue(result.contains("SubStart_1_di"), "Subprocess start event should have shape")
        assertTrue(result.contains("SubTask_1_di"), "Subprocess task should have shape")
        assertTrue(result.contains("SubEnd_1_di"), "Subprocess end event should have shape")
    }
}
