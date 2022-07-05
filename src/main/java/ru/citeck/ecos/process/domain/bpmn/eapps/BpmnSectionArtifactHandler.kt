package ru.citeck.ecos.process.domain.bpmn.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import java.util.function.Consumer

@Component
class BpmnSectionArtifactHandler(
    val recordsService: RecordsService
) : EcosArtifactHandler<ObjectData> {

    override fun deleteArtifact(artifactId: String) {
        recordsService.delete(RecordRef.create("bpmn-section-repo", artifactId))
    }

    override fun deployArtifact(artifact: ObjectData) {
        recordsService.mutate(
            RecordRef.create("bpmn-section-repo", ""),
            mapOf(
                "id" to artifact["id"],
                "name" to artifact["name"]
            )
        )
    }

    override fun getArtifactType(): String {
        return "process/bpmn-section"
    }

    override fun listenChanges(listener: Consumer<ObjectData>) {
    }
}
