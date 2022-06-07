package ru.citeck.ecos.process.domain.procdef

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.apps.app.domain.artifact.source.ArtifactSourceType
import ru.citeck.ecos.apps.app.domain.artifact.source.SourceKey
import ru.citeck.ecos.apps.app.service.LocalAppService
import ru.citeck.ecos.apps.artifact.ArtifactService
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import java.time.Instant

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class EappsTest {

    @Autowired
    lateinit var localAppService: LocalAppService

    @Autowired
    lateinit var artifactService: ArtifactService

    @Test
    fun cmmnControllerTest() {
        testProcessController("process/cmmn", "general-case-template.xml")
    }

    @Test
    fun bpmnControllerTest() {
        testProcessController("process/bpmn", "test-process.bpmn.xml")
    }

    private fun testProcessController(artifactType: String, expectedMetaId: String) {
        val typesDir = localAppService.getArtifactTypesDir()

        val artifactsDir = localAppService.getArtifactsDir(
            SourceKey("classpath", ArtifactSourceType.APPLICATION),
            typesDir,
            Instant.now()
        )

        val cmmnTypeCtx = artifactService.getType(artifactType)!!
        val artifacts = artifactService.readArtifacts(artifactsDir, artifactService.loadTypes(typesDir))

        assertEquals(1, artifacts[artifactType]!!.size)
        val meta = artifactService.getArtifactMeta(cmmnTypeCtx, artifacts[artifactType]!![0])!!

        assertEquals(expectedMetaId, meta.id)
    }
}
