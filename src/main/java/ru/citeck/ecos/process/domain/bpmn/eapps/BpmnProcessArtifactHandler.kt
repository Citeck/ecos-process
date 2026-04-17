package ru.citeck.ecos.process.domain.bpmn.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler
import ru.citeck.ecos.apps.artifact.controller.type.binary.BinArtifact
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DEF_STATE
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnRefsNormalizer
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.BiConsumer

private const val ARTIFACT_TYPE = "process/bpmn"

@Component
class BpmnProcessArtifactHandler(
    private val recordsService: RecordsService,
    private val procDefService: ProcDefService,
    private val bpmnProcessDefRecords: BpmnProcessDefRecords,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val bpmnIO: BpmnIO,
    private val workspaceService: WorkspaceService
) : WsAwareArtifactHandler<BinArtifact> {

    override fun deleteArtifact(artifactId: String, workspace: String) {
        throw IllegalStateException("deleteArtifact not yet implemented (artifactId='$artifactId', workspace='$workspace')")
    }

    override fun getArtifactType(): String {
        return ARTIFACT_TYPE
    }

    override fun listenChanges(listener: BiConsumer<BinArtifact, String>) {
        procDefService.listenChanges(BPMN_PROC_TYPE) { dto ->

            val rev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, dto.revisionId)
                ?: error("Revision not found for procDef: ${dto.id}")

            val data = if (rev.dataState == ProcDefRevDataState.RAW) {
                String(rev.loadData(procDefRevDataProvider))
            } else {
                validateFormatAndGetEcosBpmnString(rev.loadData(procDefRevDataProvider))
            }

            val strippedData = stripWorkspace(data)

            listener.accept(
                BinArtifact("${rev.procDefId}.bpmn.xml", ObjectData.create(), strippedData.toByteArray()),
                dto.workspace ?: ""
            )
        }
    }

    private fun stripWorkspace(xml: String): String {
        val definitions = BpmnXmlUtils.readFromString(xml)
        BpmnRefsNormalizer.removeWorkspaceAttr(definitions)
        BpmnRefsNormalizer.stripRefs(definitions, workspaceService)
        return BpmnXmlUtils.writeToString(definitions)
    }

    private fun validateFormatAndGetEcosBpmnString(revData: ByteArray): String {
        val ecosBpmnDef = bpmnIO.importEcosBpmn(String(revData))
        bpmnIO.exportCamundaBpmn(ecosBpmnDef)

        return bpmnIO.exportEcosBpmnToString(ecosBpmnDef)
    }

    override fun deployArtifact(artifact: BinArtifact, workspace: String) {

        // CURRENT_WS placeholders inside XML ref attributes (ecosType, formRef, notificationTemplate, etc.)
        // are rebound to the target workspace by `BpmnRefsNormalizer.bindRefs`, invoked from
        // `BpmnMutateDataProcessor.prepareDefinitionForMutateData` during the records-service mutate —
        // no need to duplicate that logic here.
        val stringDef = String(artifact.data)
        val definition = BpmnXmlUtils.readFromString(stringDef)
        val isRAW = definition.otherAttributes[BPMN_PROP_DEF_STATE] == ProcDefRevDataState.RAW.name

        val bpmnMutateRecord = bpmnProcessDefRecords.BpmnMutateRecord(
            id = "",
            processDefId = "",
            name = MLText.EMPTY,
            ecosType = EntityRef.EMPTY,
            formRef = EntityRef.EMPTY,
            workingCopySourceRef = EntityRef.EMPTY,
            definition = stringDef,
            enabled = false,
            autoStartEnabled = false,
            autoDeleteEnabled = true,
            action = if (isRAW) {
                BpmnProcessDefActions.DRAFT.name
            } else {
                BpmnProcessDefActions.DEPLOY.name
            },
            sectionRef = EntityRef.EMPTY,
            imageBytes = null,
            workspace = workspace
        )

        recordsService.mutate("${AppName.EPROC}/${BpmnProcessDefRecords.ID}@", bpmnMutateRecord)
    }
}
