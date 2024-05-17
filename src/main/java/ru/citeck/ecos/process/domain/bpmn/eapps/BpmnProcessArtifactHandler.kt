package ru.citeck.ecos.process.domain.bpmn.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.apps.artifact.controller.type.binary.BinArtifact
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcessDefRecords
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_DEF_STATE
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Consumer

private const val ARTIFACT_TYPE = "process/bpmn"

@Component
class BpmnProcessArtifactHandler(
    private val recordsService: RecordsService,
    private val procDefService: ProcDefService,
    private val bpmnProcessDefRecords: BpmnProcessDefRecords
) : EcosArtifactHandler<BinArtifact> {

    override fun deleteArtifact(artifactId: String) {
        throw IllegalStateException("not yet implemented")
    }

    override fun getArtifactType(): String {
        return ARTIFACT_TYPE
    }

    override fun listenChanges(listener: Consumer<BinArtifact>) {
        procDefService.listenChanges(BPMN_PROC_TYPE) { dto ->

            val rev = procDefService.getProcessDefRev(BPMN_PROC_TYPE, dto.revisionId)
                ?: error("Revision not found for procDef: ${dto.id}")

            val data = if (rev.dataState == ProcDefRevDataState.RAW) {
                String(rev.data)
            } else {
                validateFormatAndGetEcosBpmnString(rev.data)
            }

            listener.accept(BinArtifact("${rev.procDefId}.bpmn.xml", ObjectData.create(), data.toByteArray()))
        }
    }

    private fun validateFormatAndGetEcosBpmnString(revData: ByteArray): String {
        val ecosBpmnDef = BpmnIO.importEcosBpmn(String(revData))
        BpmnIO.exportCamundaBpmn(ecosBpmnDef)

        return BpmnIO.exportEcosBpmnToString(ecosBpmnDef)
    }

    override fun deployArtifact(artifact: BinArtifact) {

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
            imageBytes = null
        )

        recordsService.mutate("${AppName.EPROC}/${BpmnProcessDefRecords.ID}@", bpmnMutateRecord)
    }
}
