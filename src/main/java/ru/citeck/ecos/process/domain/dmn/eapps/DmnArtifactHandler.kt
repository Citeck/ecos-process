package ru.citeck.ecos.process.domain.dmn.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.apps.artifact.controller.type.binary.BinArtifact
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.dmn.DMN_PROC_TYPE
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_DEF_RECORDS_SOURCE_ID
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDefActions
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDefRecords
import ru.citeck.ecos.process.domain.dmn.io.DmnIO
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Consumer

private const val ARTIFACT_TYPE = "process/dmn"

@Component
class DmnArtifactHandler(
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
        procDefService.listenChanges(DMN_PROC_TYPE) { dto ->

            val rev = procDefService.getProcessDefRev(DMN_PROC_TYPE, dto.revisionId)
                ?: error("Revision not found for procDef: ${dto.id}")

            val data = validateFormatAndGetDmnString(rev.data)

            listener.accept(
                BinArtifact(
                    "${rev.procDefId}$DMN_RESOURCE_NAME_POSTFIX.xml",
                    ObjectData.create(),
                    data.toByteArray()
                )
            )
        }
    }

    private fun validateFormatAndGetDmnString(revData: ByteArray): String {
        val dmnDef = DmnIO.importEcosDmn(String(revData))
        DmnIO.exportCamundaDmn(dmnDef)

        return DmnIO.exportCamundaDmnToString(dmnDef)
    }

    override fun deployArtifact(artifact: BinArtifact) {

        val stringDef = String(artifact.data)

        val dmnMutateRecord = DmnDefRecords.DmnMutateRecord(
            id = "",
            defId = "",
            name = MLText.EMPTY,
            definition = stringDef,
            action = DmnDefActions.DEPLOY.toString(),
            sectionRef = EntityRef.EMPTY,
            imageBytes = null
        )

        recordsService.mutate("${AppName.EPROC}/$DMN_DEF_RECORDS_SOURCE_ID@", dmnMutateRecord)
    }
}
