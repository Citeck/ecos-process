package ru.citeck.ecos.process.domain.dmnsection.eapps

import org.springframework.stereotype.Component
import ru.citeck.ecos.apps.app.domain.handler.EcosArtifactHandler
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.domain.dmnsection.config.DMN_SECTIONS_RECORDS_ID
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.function.Consumer

const val DMN_SECTION_TYPE = "dmn-section"

@Component
class DmnSectionArtifactHandler(
    val recordsService: RecordsService
) : EcosArtifactHandler<ObjectData> {

    override fun deleteArtifact(artifactId: String) {
        recordsService.delete(EntityRef.create(DMN_SECTION_TYPE, artifactId))
    }

    override fun deployArtifact(artifact: ObjectData) {
        recordsService.mutate(
            EntityRef.create(DMN_SECTIONS_RECORDS_ID, ""),
            mapOf(
                "id" to artifact["id"],
                "name" to artifact["name"],
                "parentRef" to artifact["parentRef"]
            )
        )
    }

    override fun getArtifactType(): String {
        return "process/$DMN_SECTION_TYPE"
    }

    override fun listenChanges(listener: Consumer<ObjectData>) {
    }
}
