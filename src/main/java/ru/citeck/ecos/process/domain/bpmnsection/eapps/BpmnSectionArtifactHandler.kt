package ru.citeck.ecos.process.domain.bpmnsection.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.bpmnsection.config.BpmnSectionConfig
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import java.util.function.Consumer

@Component
class BpmnSectionArtifactHandler(
    val recordsService: RecordsService
) : EcosArtifactHandler<ObjectData> {

    override fun deleteArtifact(artifactId: String) {
        recordsService.delete(RecordRef.create(BpmnSectionConfig.BPMN_SECTION_REPO_SOURCE_ID, artifactId))
    }

    override fun deployArtifact(artifact: ObjectData) {
        recordsService.mutate(
            RecordRef.create(BpmnSectionConfig.BPMN_SECTION_REPO_SOURCE_ID, ""),
            mapOf(
                "id" to artifact["id"],
                "name" to artifact["name"],
                "parentRef" to artifact["parentRef"]
            )
        )
    }

    override fun getArtifactType(): String {
        return "process/bpmn-section"
    }

    override fun listenChanges(listener: Consumer<ObjectData>) {
    }
}
