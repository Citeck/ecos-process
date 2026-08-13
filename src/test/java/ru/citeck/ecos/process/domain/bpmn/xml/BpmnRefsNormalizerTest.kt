package ru.citeck.ecos.process.domain.bpmn.xml

import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_FORM_REF
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NOTIFICATION_TEMPLATE
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnRefsNormalizer
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSendTask
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.webapp.api.entity.EntityRef
import kotlin.test.Test

class BpmnRefsNormalizerTest {

    private val targetWs = "wsAlpha"
    private val targetWsSysId = "alpha"
    private val foreignWsSysId = "beta"

    private val workspaceService = fakeWorkspaceService()

    @Test
    fun `bindRefs - CURRENT_WS placeholder is replaced with target ws sysId`() {
        val xml = bpmnXml(
            ecosType = "emodel/type@CURRENT_WS:my-type",
            formRef = "uiserv/eform@CURRENT_WS:my-form",
            notificationTemplate = "notifications/template@CURRENT_WS:my-template"
        )
        val def = BpmnXmlUtils.readFromString(xml)

        BpmnRefsNormalizer.bindRefs(def, targetWs, workspaceService)

        assertThat(ecosType(def)).isEqualTo("emodel/type@$targetWsSysId:my-type")
        assertThat(formRef(def)).isEqualTo("uiserv/eform@$targetWsSysId:my-form")
        assertThat(notificationTemplate(def)).isEqualTo("notifications/template@$targetWsSysId:my-template")
    }

    @Test
    fun `bindRefs - unprefixed ref is left alone when not co-deployed`() {
        val xml = bpmnXml(ecosType = "emodel/type@my-type")
        val def = BpmnXmlUtils.readFromString(xml)

        BpmnRefsNormalizer.bindRefs(def, targetWs, workspaceService)

        assertThat(ecosType(def)).isEqualTo("emodel/type@my-type")
    }

    @Test
    fun `bindRefs - unprefixed ref is promoted when co-deployed in target ws`() {
        val xml = bpmnXml(
            ecosType = "emodel/type@my-type",
            formRef = "uiserv/eform@my-form",
            notificationTemplate = "notifications/template@my-template"
        )
        val def = BpmnXmlUtils.readFromString(xml)

        val coDeployed = setOf(
            EntityRef.valueOf("emodel/type@my-type"),
            EntityRef.valueOf("uiserv/eform@my-form")
        )

        BpmnRefsNormalizer.bindRefs(def, targetWs, workspaceService, coDeployed)

        assertThat(ecosType(def)).isEqualTo("emodel/type@$targetWsSysId:my-type")
        assertThat(formRef(def)).isEqualTo("uiserv/eform@$targetWsSysId:my-form")
        // notificationTemplate is not in co-deployed set — stays unprefixed (treated as global)
        assertThat(notificationTemplate(def)).isEqualTo("notifications/template@my-template")
    }

    @Test
    fun `bindRefs - ref with foreign ws prefix is not double-prefixed`() {
        val xml = bpmnXml(ecosType = "emodel/type@$foreignWsSysId:my-type")
        val def = BpmnXmlUtils.readFromString(xml)

        BpmnRefsNormalizer.bindRefs(def, targetWs, workspaceService)

        assertThat(ecosType(def)).isEqualTo("emodel/type@$foreignWsSysId:my-type")
    }

    @Test
    fun `bindRefs - blank (global) workspace strips CURRENT_WS to bare and keeps co-deployed refs global`() {
        // Importing an ecos-app into the global admin deploys with a blank workspace. CURRENT_WS
        // placeholders must still be resolved — stripped to a bare ref for a global deploy —
        // otherwise the unresolved "CURRENT_WS:" prefix is persisted verbatim. An unprefixed
        // co-deployed ref must stay global: there is no ws prefix to add. The ecosType assertion
        // is what discriminates against a regression of the removed `if (workspace.isBlank()) return`
        // guard — under the guard bindRefs exits early and CURRENT_WS:my-type survives.
        val xml = bpmnXml(
            ecosType = "emodel/type@CURRENT_WS:my-type",
            formRef = "uiserv/eform@my-form"
        )
        val def = BpmnXmlUtils.readFromString(xml)

        BpmnRefsNormalizer.bindRefs(def, "", workspaceService, setOf(EntityRef.valueOf("uiserv/eform@my-form")))

        // CURRENT_WS placeholder stripped to bare (fails if the blank-ws guard is restored)
        assertThat(ecosType(def)).isEqualTo("emodel/type@my-type")
        // co-deployed but unprefixed → stays global, not promoted (no prefix for a global ws)
        assertThat(formRef(def)).isEqualTo("uiserv/eform@my-form")
    }

    @Test
    fun `bindRefs - CURRENT_WS placeholder wins over coDeployed match`() {
        // A ref already carrying CURRENT_WS:my-type goes through the placeholder path,
        // not the unprefixed promotion path — even if a colliding co-deployed ref is present.
        val xml = bpmnXml(ecosType = "emodel/type@CURRENT_WS:my-type")
        val def = BpmnXmlUtils.readFromString(xml)

        val coDeployed = setOf(EntityRef.valueOf("emodel/type@my-type"))

        BpmnRefsNormalizer.bindRefs(def, targetWs, workspaceService, coDeployed)

        assertThat(ecosType(def)).isEqualTo("emodel/type@$targetWsSysId:my-type")
    }

    private fun bpmnXml(
        ecosType: String = "",
        formRef: String = "",
        notificationTemplate: String = ""
    ): String = """
        <?xml version="1.0" encoding="UTF-8"?>
        <bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
                          xmlns:ecos="http://www.citeck.ru/ecos/bpmn/1.0"
                          id="Definitions_1"
                          targetNamespace="http://bpmn.io/schema/bpmn"
                          ecos:ecosType="$ecosType">
          <bpmn:process id="proc-1" isExecutable="true">
            <bpmn:userTask id="UserTask_1" ecos:formRef="$formRef"/>
            <bpmn:sendTask id="SendTask_1" ecos:notificationTemplate="$notificationTemplate"/>
          </bpmn:process>
        </bpmn:definitions>
    """.trimIndent()

    private fun ecosType(def: TDefinitions): String = def.otherAttributes[BPMN_PROP_ECOS_TYPE] ?: ""

    private fun userTask(def: TDefinitions): TUserTask = (def.rootElement.first { it.value is TProcess }.value as TProcess)
        .flowElement.first { it.value is TUserTask }.value as TUserTask

    private fun sendTask(def: TDefinitions): TSendTask = (def.rootElement.first { it.value is TProcess }.value as TProcess)
        .flowElement.first { it.value is TSendTask }.value as TSendTask

    private fun formRef(def: TDefinitions): String = userTask(def).otherAttributes[BPMN_PROP_FORM_REF] ?: ""

    private fun notificationTemplate(def: TDefinitions): String = sendTask(def).otherAttributes[BPMN_PROP_NOTIFICATION_TEMPLATE] ?: ""

    private fun fakeWorkspaceService(): WorkspaceService {
        val ws = Mockito.mock(WorkspaceService::class.java)
        whenever(ws.replaceCurrentWsPlaceholderToWsPrefix(any(), any())).thenAnswer { inv ->
            val id = inv.getArgument<String>(0)
            val targetWorkspace = inv.getArgument<String>(1)
            if (!id.startsWith("CURRENT_WS${IdInWs.WS_DELIM}")) {
                id
            } else {
                val sysId = sysIdFor(targetWorkspace)
                // mirror real getPrefixForIdInWorkspace: empty prefix (no delimiter) for a global ws
                val prefix = if (sysId.isEmpty()) "" else "$sysId${IdInWs.WS_DELIM}"
                prefix + id.substring("CURRENT_WS${IdInWs.WS_DELIM}".length)
            }
        }
        whenever(ws.addWsPrefixToId(any(), any())).thenAnswer { inv ->
            val localId = inv.getArgument<String>(0)
            val workspace = inv.getArgument<String>(1)
            val sysId = sysIdFor(workspace)
            if (sysId.isEmpty() || localId.startsWith("$sysId${IdInWs.WS_DELIM}")) {
                localId
            } else {
                "$sysId${IdInWs.WS_DELIM}$localId"
            }
        }
        return ws
    }

    private fun sysIdFor(workspace: String): String = when (workspace) {
        targetWs -> targetWsSysId
        "" -> ""
        else -> workspace
    }
}
