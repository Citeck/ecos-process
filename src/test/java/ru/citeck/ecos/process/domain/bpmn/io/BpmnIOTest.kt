package ru.citeck.ecos.process.domain.bpmn.io

import jakarta.xml.bind.JAXBElement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import ru.citeck.ecos.commons.utils.resource.ResourceUtils
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.model.lib.workspace.api.WorkspaceApi
import ru.citeck.ecos.model.lib.workspace.api.WsMembershipType
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaProperties
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCallActivity
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TServiceTask

class BpmnIOTest {

    lateinit var bpmnIO: BpmnIO

    @BeforeEach
    fun init() {
        val modelServices = ModelServiceFactory()
        modelServices.setWorkspaceApi(object : WorkspaceApi {
            override fun getNestedWorkspaces(workspaces: Collection<String>): List<Set<String>> {
                return workspaces.map { emptySet() }
            }
            override fun getUserWorkspaces(user: String, membershipType: WsMembershipType): Set<String> {
                return emptySet()
            }
            override fun isUserManagerOf(user: String, workspace: String): Boolean {
                return false
            }
            override fun mapIdentifiers(
                identifiers: List<String>,
                mappingType: WorkspaceApi.IdMappingType
            ): List<String> {
                return when (mappingType) {
                    WorkspaceApi.IdMappingType.WS_ID_TO_SYS_ID -> identifiers.map {
                        "$it-sys"
                    }
                    WorkspaceApi.IdMappingType.WS_SYS_ID_TO_ID -> identifiers.map {
                        it.replace("-sys", "")
                    }
                    else -> identifiers
                }
            }
        })
        bpmnIO = BpmnIO(modelServices.workspaceService)
    }

    private inline fun <reified T : TBaseElement> findElementById(elements: Collection<JAXBElement<*>>, id: String): T? {
        for (jaxbElem in elements) {
            val value = jaxbElem.value
            if (value is T && value.id == id) {
                return value
            }
        }
        return null
    }

    @Test
    fun `send task recipients send strategy round trip`() {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/elements/sendtask/test-send-task-recipients-per-recipient.bpmn.xml"
        ).readText()

        val bpmnDef = bpmnIO.importEcosBpmn(testDef)

        val sendTask = bpmnDef.process.first().flowElements.first { it.id == "sendTask" }
        assertThat(sendTask.data["recipientsSendStrategy"].asText()).isEqualTo("PER_RECIPIENT")

        val exportedXml = bpmnIO.exportEcosBpmnToString(bpmnDef)
        assertThat(exportedXml).contains("notificationRecipientsStrategy=\"PER_RECIPIENT\"")
    }

    @Test
    fun `send task per role recipients send strategy round trip`() {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/elements/sendtask/test-send-task-recipients-per-role.bpmn.xml"
        ).readText()

        val bpmnDef = bpmnIO.importEcosBpmn(testDef)

        val sendTask = bpmnDef.process.first().flowElements.first { it.id == "sendTask" }
        assertThat(sendTask.data["recipientsSendStrategy"].asText()).isEqualTo("PER_ROLE")

        val exportedXml = bpmnIO.exportEcosBpmnToString(bpmnDef)
        assertThat(exportedXml).contains("notificationRecipientsStrategy=\"PER_ROLE\"")
    }

    @Test
    fun `send task without recipients send strategy defaults to combined`() {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/elements/sendtask/test-send-task-configuration-check.bpmn.xml"
        ).readText()

        val bpmnDef = bpmnIO.importEcosBpmn(testDef)

        val sendTask = bpmnDef.process.first().flowElements.first { it.id == "sendTask" }
        assertThat(sendTask.data["recipientsSendStrategy"].asText()).isEqualTo("COMBINED")

        // default strategy must not leak into the exported xml of existing processes
        val exportedXml = bpmnIO.exportEcosBpmnToString(bpmnDef)
        assertThat(exportedXml).doesNotContain("notificationRecipientsStrategy")
    }

    @Test
    fun `per recipient strategy with non empty cc is rejected on import`() {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/elements/sendtask/test-send-task-per-recipient-with-cc.bpmn.xml"
        ).readText()

        assertThrows<EcosBpmnElementDefinitionException> {
            bpmnIO.importEcosBpmn(testDef)
        }
    }

    @Test
    fun `per role strategy with non empty cc is rejected on import`() {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/elements/sendtask/test-send-task-per-role-with-cc.bpmn.xml"
        ).readText()

        assertThrows<EcosBpmnElementDefinitionException> {
            bpmnIO.importEcosBpmn(testDef)
        }
    }

    @Test
    fun testCamundaExportWithWorkspace() {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/io/export-for-camunda-test.bpmn.xml"
        ).readText()
        val bpmnDef = bpmnIO.importEcosBpmn(testDef)
        val bpmnWithWs = bpmnDef.copy(workspace = "tws0")

        val exportRes = bpmnIO.exportCamundaBpmn(bpmnWithWs)

        assertThat(exportRes.otherAttributes[BPMN_PROP_WORKSPACE]).isEqualTo("tws0")

        fun findProcById(id: String): TProcess? = findElementById(exportRes.rootElement, id)

        val mainProc = findProcById("tws0-sys..export-for-camunda-test")
        assertThat(mainProc).isNotNull
        assertThat(findProcById("tws0-sys..non_main_process_1")).isNotNull
        assertThat(findProcById("non_main_process_1")).isNull()

        val callActivity: TCallActivity = findElementById(mainProc!!.flowElement, "Activity_1jkg58z")!!

        val calledElem = callActivity.calledElement.localPart
        assertThat(calledElem).isEqualTo("tws0-sys..non_main_process_1")
    }

    private fun exportAiTaskToCamunda(resourceName: String): TDefinitions {
        val testDef = ResourceUtils.getFile(
            "classpath:test/bpmn/elements/aitask/$resourceName.bpmn.xml"
        ).readText()
        return bpmnIO.exportCamundaBpmn(bpmnIO.importEcosBpmn(testDef))
    }

    private fun camundaPropertiesOfTask(camundaDef: TDefinitions, taskId: String): Map<String, String> {
        val process = camundaDef.rootElement.map { it.value }.filterIsInstance<TProcess>().first()
        val task: TServiceTask = findElementById(process.flowElement, taskId)!!
        return task.extensionElements.any
            .filterIsInstance<JAXBElement<*>>()
            .map { it.value }
            .filterIsInstance<CamundaProperties>()
            .flatMap { it.properties }
            .associate { it.name to it.value }
    }

    @Test
    fun `ai task exports enabled add document to context as camunda property`() {
        val camundaDef = exportAiTaskToCamunda("test-ai-task-add-document-enabled")

        assertThat(camundaPropertiesOfTask(camundaDef, "aiTask"))
            .containsEntry("aiAddDocumentToContext", "true")
    }

    @Test
    fun `ai task exports disabled add document to context as camunda property`() {
        val camundaDef = exportAiTaskToCamunda("test-ai-task-add-document-disabled")

        assertThat(camundaPropertiesOfTask(camundaDef, "aiTask"))
            .containsEntry("aiAddDocumentToContext", "false")
    }

    // The AI task handler in citeck-ai resolves aiAddDocumentToContext from camunda extension
    // properties. The property has to reach the deployed definition even when the source schema
    // omits the attribute, otherwise the handler applies its own default instead of the one
    // declared in the task form.
    @Test
    fun `ai task without add document to context attribute still exports the camunda property`() {
        val camundaDef = exportAiTaskToCamunda("test-ai-task-without-add-document-attribute")

        assertThat(camundaPropertiesOfTask(camundaDef, "aiTask"))
            .containsEntry("aiAddDocumentToContext", "true")
    }

    private fun importInclusiveGatewayWithDefault() = bpmnIO.importEcosBpmn(
        ResourceUtils.getFile(
            "classpath:test/bpmn/elements/gateway/test-inclusive-gateway-with-default.bpmn.xml"
        ).readText()
    )

    @Test
    fun `inclusive gateway default flow survives ecos export`() {
        val bpmnDef = importInclusiveGatewayWithDefault()

        val gateway = bpmnDef.process.first().flowElements.first { it.id == "inclusive_gateway" }
        assertThat(gateway.data["default"].asText()).isEqualTo("Flow_0mcybpj")

        val exportedXml = bpmnIO.exportEcosBpmnToString(bpmnDef)
        assertThat(exportedXml).contains("default=\"Flow_0mcybpj\"")
    }

    @Test
    fun `inclusive gateway default flow survives camunda export`() {
        val bpmnDef = importInclusiveGatewayWithDefault()

        val exportedXml = bpmnIO.exportCamundaBpmnToString(bpmnDef)
        assertThat(exportedXml).contains("default=\"Flow_0mcybpj\"")
    }

    @Test
    fun `unresolvable default flow fails export with gateway and flow ids in message`() {
        val bpmnDef = importInclusiveGatewayWithDefault()
        bpmnDef.process.first().flowElements.first { it.id == "inclusive_gateway" }
            .data["default"] = "missing_flow"

        val exception = assertThrows<IllegalStateException> {
            bpmnIO.exportEcosBpmnToString(bpmnDef)
        }
        assertThat(exception.message).contains("inclusive_gateway")
        assertThat(exception.message).contains("missing_flow")
    }

    @Test
    fun `default flow referencing non sequence flow element fails export`() {
        val bpmnDef = importInclusiveGatewayWithDefault()
        bpmnDef.process.first().flowElements.first { it.id == "inclusive_gateway" }
            .data["default"] = "end_all_event"

        val exception = assertThrows<IllegalStateException> {
            bpmnIO.exportEcosBpmnToString(bpmnDef)
        }
        assertThat(exception.message).contains("inclusive_gateway")
        assertThat(exception.message).contains("end_all_event")
    }
}
