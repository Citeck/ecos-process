package ru.citeck.ecos.process.domain.dmn.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.WsAwareArtifactHandler
import ru.citeck.ecos.apps.artifact.controller.type.binary.BinArtifact
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.dmn.DMN_PROC_TYPE
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_DEF_RECORDS_SOURCE_ID
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_RESOURCE_NAME_POSTFIX
import ru.citeck.ecos.process.domain.dmn.api.records.DmnDefActions
import ru.citeck.ecos.process.domain.dmn.io.DMN_PROP_WORKSPACE
import ru.citeck.ecos.process.domain.dmn.io.DmnIO
import ru.citeck.ecos.process.domain.dmn.io.xml.DmnXmlUtils
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataProvider
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.BiConsumer

private const val ARTIFACT_TYPE = "process/dmn"

@Component
class DmnArtifactHandler(
    private val recordsService: RecordsService,
    private val procDefService: ProcDefService,
    private val procDefRevDataProvider: ProcDefRevDataProvider,
    private val dmnIO: DmnIO
) : WsAwareArtifactHandler<BinArtifact> {

    override fun deleteArtifact(artifactId: String, workspace: String) {
        throw IllegalStateException("deleteArtifact not yet implemented (artifactId='$artifactId', workspace='$workspace')")
    }

    override fun getArtifactType(): String {
        return ARTIFACT_TYPE
    }

    override fun listenChanges(listener: BiConsumer<BinArtifact, String>) {
        procDefService.listenChanges(DMN_PROC_TYPE) { dto ->

            val rev = procDefService.getProcessDefRev(DMN_PROC_TYPE, dto.revisionId)
                ?: error("Revision not found for procDef: ${dto.id}")

            val data = validateFormatAndGetDmnString(rev.loadData(procDefRevDataProvider))
            val strippedData = stripWorkspace(data)

            listener.accept(
                BinArtifact(
                    "${rev.procDefId}$DMN_RESOURCE_NAME_POSTFIX.xml",
                    ObjectData.create(),
                    strippedData.toByteArray()
                ),
                dto.workspace ?: ""
            )
        }
    }

    private fun stripWorkspace(xml: String): String {
        val definitions = DmnXmlUtils.readFromString(xml)
        definitions.otherAttributes.remove(DMN_PROP_WORKSPACE)
        return DmnXmlUtils.writeToString(definitions)
    }

    private fun validateFormatAndGetDmnString(revData: ByteArray): String {
        val dmnDef = dmnIO.importEcosDmn(String(revData))
        dmnIO.exportCamundaDmn(dmnDef)

        return dmnIO.exportCamundaDmnToString(dmnDef)
    }

    override fun deployArtifact(artifact: BinArtifact, workspace: String) {

        val stringDef = String(artifact.data)

        val dmnMutateRecord = DataValue.createObj()
            .set("id", "")
            .set("defId", "")
            .set("name", MLText.EMPTY)
            .set("definition", stringDef)
            .set("action", DmnDefActions.DEPLOY.toString())
            .set("sectionRef", EntityRef.EMPTY)
            .set(RecordConstants.ATT_WORKSPACE, workspace)

        recordsService.mutate("${AppName.EPROC}/$DMN_DEF_RECORDS_SOURCE_ID@", dmnMutateRecord)
    }
}
