package ru.citeck.ecos.process.domain.bpmn.api.records

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.workspace.IdInWs
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

@Component
class BpmnMutateDataProcessor(
    private val procDefService: ProcDefService,
    private val workspaceService: WorkspaceService,
    private val bpmnIO: BpmnIO
) {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun getCompletedMutateData(mutateRecord: BpmnProcessDefRecords.BpmnMutateRecord): BpmnMutateData {

        var (statedInitialDefinition, saveAsDraft) = mutateRecord.getStatedDefinition()

        var recordId = mutateRecord.id
        var processDefId = mutateRecord.processDefId
        var name = mutateRecord.name
        var workingCopySourceRef = EntityRef.EMPTY
        var autoStartEnabled = mutateRecord.autoStartEnabled
        var autoDeleteEnabled = mutateRecord.autoDeleteEnabled
        var enabled = mutateRecord.enabled
        val workspace = mutateRecord.workspace

        if (mutateRecord.isModuleCopy()) {
            recordId = workspaceService.addWsPrefixToId(
                mutateRecord.moduleId ?: error("moduleId is missing"),
                workspace,
            )
            processDefId = recordId.substringAfter(IdInWs.WS_DELIM)

            val procDef = BpmnXmlUtils.readFromString(statedInitialDefinition)

            procDef.otherAttributes[BPMN_PROP_ENABLED] = false.toString()
            procDef.otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = false.toString()
            procDef.otherAttributes[BPMN_PROP_AUTO_DELETE_ENABLED] = true.toString()
            procDef.otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = processDefId

            val lastRevisionId: UUID = getLastRevisionId(workspace, mutateRecord.id)
            workingCopySourceRef = EntityRef.create(
                AppName.EPROC,
                BpmnProcessDefVersionRecords.ID,
                lastRevisionId.toString()
            )

            procDef.otherAttributes[BPMN_PROP_WORKING_COPY_SOURCE_REF] = workingCopySourceRef.toString()

            statedInitialDefinition = BpmnXmlUtils.writeToString(procDef)
            autoStartEnabled = false
            autoDeleteEnabled = true
            enabled = false
        }

        var newEcosDefinition = ""
        var ecosType = mutateRecord.ecosType
        var formRef = mutateRecord.formRef
        var sectionRef = mutateRecord.sectionRef

        var newCamundaDefinitionStr = ""

        if (statedInitialDefinition.isNotBlank()) {
            if (saveAsDraft) {
                // parse definition data from raw bpmn

                val draftDefinition = BpmnXmlUtils.readFromString(statedInitialDefinition)

                ecosType = EntityRef.valueOf(draftDefinition.otherAttributes[BPMN_PROP_ECOS_TYPE])
                formRef = EntityRef.valueOf(draftDefinition.otherAttributes[BPMN_PROP_FORM_REF])
                sectionRef = EntityRef.valueOf(draftDefinition.otherAttributes[BPMN_PROP_SECTION_REF])
                name =
                    Json.mapper.convert(draftDefinition.otherAttributes[BPMN_PROP_NAME_ML], MLText::class.java)
                        ?: MLText()
                processDefId = draftDefinition.otherAttributes[BPMN_PROP_PROCESS_DEF_ID]!!
                enabled = draftDefinition.otherAttributes[BPMN_PROP_ENABLED].toBoolean()
                autoStartEnabled = draftDefinition.otherAttributes[BPMN_PROP_AUTO_START_ENABLED].toBoolean()
                autoDeleteEnabled = draftDefinition.otherAttributes[BPMN_PROP_AUTO_DELETE_ENABLED].toBoolean()

                draftDefinition.otherAttributes[BPMN_PROP_WORKSPACE] = workspace

                newEcosDefinition = statedInitialDefinition
            } else {
                // Parse definition data from Ecos BPMN format

                bpmnIO.validateEcosBpmnFormat(statedInitialDefinition)

                var ecosBpmnDefinition = bpmnIO.importEcosBpmn(statedInitialDefinition)

                debugLogEcosAndCamundaDefStr(ecosBpmnDefinition)

                ecosType = ecosBpmnDefinition.ecosType
                formRef = ecosBpmnDefinition.formRef
                sectionRef = ecosBpmnDefinition.sectionRef
                name = ecosBpmnDefinition.name
                processDefId = ecosBpmnDefinition.id
                enabled = ecosBpmnDefinition.enabled
                autoStartEnabled = ecosBpmnDefinition.autoStartEnabled
                autoDeleteEnabled = ecosBpmnDefinition.autoDeleteEnabled

                ecosBpmnDefinition = ecosBpmnDefinition.copy(workspace = workspace)

                newEcosDefinition = bpmnIO.exportEcosBpmnToString(ecosBpmnDefinition)
                newCamundaDefinitionStr = bpmnIO.exportCamundaBpmnToString(ecosBpmnDefinition)
            }

            if (recordId.isBlank()) {
                recordId = workspaceService.addWsPrefixToId(processDefId, workspace)
            }
        }

        if (processDefId.isBlank()) {
            error("processDefId is missing")
        }

        return BpmnMutateData(
            recordId = recordId,
            processDefId = processDefId,
            workspace = workspace,
            name = name,
            ecosType = ecosType,
            formRef = formRef,
            workingCopySourceRef = workingCopySourceRef,
            enabled = enabled,
            autoStartEnabled = autoStartEnabled,
            autoDeleteEnabled = autoDeleteEnabled,
            sectionRef = sectionRef,
            createdFromVersion = mutateRecord.createdFromVersion,
            image = mutateRecord.imageBytes,
            newEcosDefinition = if (newEcosDefinition.isBlank()) {
                null
            } else {
                newEcosDefinition.toByteArray()
            },
            newCamundaDefinitionStr = newCamundaDefinitionStr,
            saveAsDraft = saveAsDraft
        )
    }

    private fun BpmnProcessDefRecords.BpmnMutateRecord.isModuleCopy(): Boolean {
        return !moduleId.isNullOrBlank() && id != moduleId
    }

    private fun getLastRevisionId(workspace: String, id: String): UUID {
        val ref = ProcDefRef.create(BPMN_PROC_TYPE, IdInWs.create(workspace, id))
        val currentProc = procDefService.getProcessDefById(ref) ?: error("Process definition not found: $ref")
        return currentProc.revisionId
    }

    private fun BpmnProcessDefRecords.BpmnMutateRecord.getStatedDefinition(): Pair<String, Boolean> {
        val initialDefinition: String? = when (true) {
            isModuleCopy() -> {
                val ref = ProcDefRef.create(BPMN_PROC_TYPE, IdInWs.create(workspace, processDefId))
                val currentProc = procDefService.getProcessDefById(ref) ?: error("Process definition not found: $ref")

                String(currentProc.data)
            }

            (isUploadNewVersion && !definition.isNullOrBlank()) -> {
                val procDef = BpmnXmlUtils.readFromString(definition!!)
                val procDefIdFromXml = procDef.otherAttributes[BPMN_PROP_PROCESS_DEF_ID].toString()

                if (procDefIdFromXml == id) {
                    definition
                }

                procDef.otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = id

                BpmnXmlUtils.writeToString(procDef)
            }

            else -> definition
        }

        return if (initialDefinition.isNullOrBlank()) {
            "" to false
        } else {
            val bpmnDef = BpmnXmlUtils.readFromString(initialDefinition)

            val defStateAtt = bpmnDef.otherAttributes[BPMN_PROP_DEF_STATE]
            val initialDefState = if (defStateAtt.isNullOrBlank()) {
                null
            } else {
                ProcDefRevDataState.valueOf(defStateAtt)
            }

            val isRaw = action == BpmnProcessDefActions.DRAFT.name ||
                (action.isBlank() && initialDefState == ProcDefRevDataState.RAW)

            val state = if (isRaw) {
                ProcDefRevDataState.RAW
            } else {
                ProcDefRevDataState.CONVERTED
            }

            bpmnDef.otherAttributes[BPMN_PROP_DEF_STATE] = state.name

            BpmnXmlUtils.writeToString(bpmnDef) to isRaw
        }
    }

    private fun debugLogEcosAndCamundaDefStr(ecosBpmnDef: BpmnDefinitionDef) {
        if (log.isDebugEnabled()) {
            val ecosBpmnStr = bpmnIO.exportEcosBpmnToString(ecosBpmnDef)
            log.debug { "exportEcosBpmnToString:\n$ecosBpmnStr" }

            val camundaStr = bpmnIO.exportCamundaBpmnToString(ecosBpmnDef)
            log.debug { "exportCamundaBpmnToString:\n$camundaStr" }
        }
    }
}

data class BpmnMutateData(
    val recordId: String,
    val processDefId: String,
    val workspace: String,
    val name: MLText,
    val ecosType: EntityRef,
    val formRef: EntityRef,
    val workingCopySourceRef: EntityRef = EntityRef.EMPTY,
    val enabled: Boolean,
    val autoStartEnabled: Boolean,
    val autoDeleteEnabled: Boolean,
    val sectionRef: EntityRef,
    val createdFromVersion: EntityRef = EntityRef.EMPTY,
    val image: ByteArray?,
    val newEcosDefinition: ByteArray?,
    val newCamundaDefinitionStr: String,
    val saveAsDraft: Boolean
)
