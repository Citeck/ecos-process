package ru.citeck.ecos.process.domain.procdef

import junit.framework.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import ru.citeck.ecos.apps.app.domain.artifact.source.ArtifactSourceType
import ru.citeck.ecos.apps.app.domain.artifact.source.SourceKey
import ru.citeck.ecos.apps.app.service.LocalAppService
import ru.citeck.ecos.apps.artifact.ArtifactService
import ru.citeck.ecos.process.EprocApp
import java.time.Instant

@SpringBootTest(classes = [EprocApp::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EappsTest {

    @Autowired
    lateinit var localAppService: LocalAppService
    @Autowired
    lateinit var artifactService: ArtifactService

    @Test
    fun cmmnControllerTest() {

        val typesDir = localAppService.getArtifactTypesDir()

        val artifactsDir = localAppService.getArtifactsDir(
            SourceKey("classpath", ArtifactSourceType.APPLICATION),
            typesDir,
            Instant.now()
        )

        val cmmnTypeCtx = artifactService.getType("process/cmmn")!!
        val artifacts = artifactService.readArtifacts(artifactsDir, artifactService.loadTypes(typesDir))

        assertEquals(1, artifacts["process/cmmn"]!!.size)
        val meta = artifactService.getArtifactMeta(cmmnTypeCtx, artifacts["process/cmmn"]!![0])!!

        assertEquals("general-case-template.xml", meta.id)
    }
}
