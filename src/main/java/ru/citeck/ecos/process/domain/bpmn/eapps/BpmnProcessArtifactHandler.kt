package ru.citeck.ecos.process.domain.bpmn.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.apps.artifact.controller.type.binary.BinArtifact
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefRecords
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import java.util.function.Consumer

private const val ARTIFACT_TYPE = "process/bpmn"

@Component
class BpmnProcessArtifactHandler(
    private val recordsService: RecordsService,
    private val procDefService: ProcDefService
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

            val data = validateFormatAndGetEcosBpmnString(rev.data)

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

        val bpmnMutateRecord = BpmnProcDefRecords.BpmnMutateRecord(
            id = "",
            processDefId = "",
            name = MLText.EMPTY,
            ecosType = RecordRef.EMPTY,
            formRef = RecordRef.EMPTY,
            definition = stringDef,
            enabled = false,
            autoStartEnabled = false,
            action = BpmnProcDefActions.DEPLOY.toString()
        )

        recordsService.mutate("eproc/${BpmnProcDefRecords.SOURCE_ID}@", bpmnMutateRecord)
    }
}
