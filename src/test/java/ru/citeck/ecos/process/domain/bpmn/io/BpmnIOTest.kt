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
import ru.citeck.ecos.process.domain.bpmn.model.ecos.EcosBpmnElementDefinitionException
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCallActivity
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess

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
}
