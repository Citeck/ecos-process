package ru.citeck.ecos.process.domain.bpmn.io.xml

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_FORM_REF
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_LA_ERROR_REPORT_NOTIFICATION_TEMPLATE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_LA_NOTIFICATION_TEMPLATE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_LA_SUCCESS_REPORT_NOTIFICATION_TEMPLATE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NOTIFICATION_TEMPLATE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_WORKSPACE
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TFlowElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSendTask
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSubProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TUserTask
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

/**
 * Replaces workspace prefixes in all `ecos:*` attributes that carry workspace-scoped EntityRef
 * values (ecosType on root Definitions, formRef / notificationTemplate / la*NotificationTemplate
 * on UserTask/SendTask/SubProcess trees).
 *
 * `stripRefs` — artifact leaves the source service: workspace sysId prefix → `CURRENT_WS:`.
 * `bindRefs` — artifact lands in a target workspace: `CURRENT_WS:` → target sysId prefix.
 * `removeWorkspaceAttr` — drops the `ecos:workspace` root attribute (used by artifact export only;
 * records DAO `getData` keeps it since callers may need the full definition metadata).
 *
 * Used from `BpmnProcessDefRecords.EcosBpmnRecord.prepareDefinitionForDataAtt` (records read),
 * `BpmnMutateDataProcessor.prepareDefinitionForMutateData` (records mutate), and
 * `BpmnProcessArtifactHandler.stripWorkspace` (artifact listen/emit) to keep the exact same set
 * of ref-holding attributes in sync across read/mutate/listen paths.
 */
object BpmnRefsNormalizer {

    fun stripRefs(definitions: TDefinitions, workspaceService: WorkspaceService) {
        applyToRefAttributes(definitions) { attributes, attr ->
            transform(attributes, attr) { localId ->
                workspaceService.replaceWsPrefixToCurrentWsPlaceholder(localId)
            }
        }
    }

    fun bindRefs(definitions: TDefinitions, workspace: String, workspaceService: WorkspaceService) {
        if (workspace.isBlank()) return
        applyToRefAttributes(definitions) { attributes, attr ->
            transform(attributes, attr) { localId ->
                workspaceService.replaceCurrentWsPlaceholderToWsPrefix(localId, workspace)
            }
        }
    }

    fun removeWorkspaceAttr(definitions: TDefinitions) {
        definitions.otherAttributes.remove(BPMN_PROP_WORKSPACE)
    }

    private fun applyToRefAttributes(
        definitions: TDefinitions,
        action: (MutableMap<QName, String>, QName) -> Unit
    ) {
        action(definitions.otherAttributes, BPMN_PROP_ECOS_TYPE)
        definitions.rootElement?.forEach { rootElement ->
            val element = rootElement.value
            if (element is TProcess) {
                processElements(element.flowElement, action)
            }
        }
    }

    private fun processElements(
        elements: List<JAXBElement<out TFlowElement>>?,
        action: (MutableMap<QName, String>, QName) -> Unit
    ) {
        elements?.forEach { jaxbElement ->
            when (val element = jaxbElement.value) {
                is TSendTask -> {
                    action(element.otherAttributes, BPMN_PROP_NOTIFICATION_TEMPLATE)
                }

                is TUserTask -> {
                    action(element.otherAttributes, BPMN_PROP_FORM_REF)
                    action(element.otherAttributes, BPMN_PROP_LA_NOTIFICATION_TEMPLATE)
                    action(element.otherAttributes, BPMN_PROP_LA_SUCCESS_REPORT_NOTIFICATION_TEMPLATE)
                    action(element.otherAttributes, BPMN_PROP_LA_ERROR_REPORT_NOTIFICATION_TEMPLATE)
                }

                is TSubProcess -> {
                    processElements(element.flowElement, action)
                }
            }
        }
    }

    private fun transform(
        attributes: MutableMap<QName, String>,
        attributeName: QName,
        transformLocalId: (String) -> String
    ) {
        val value = attributes[attributeName]
        if (value.isNullOrBlank()) return
        val ref = EntityRef.valueOf(value)
        attributes[attributeName] = ref.withLocalId(transformLocalId(ref.getLocalId())).toString()
    }
}
