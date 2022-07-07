package ru.citeck.ecos.process.domain.bpmn

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.bpmn.eapps.BpmnSectionArtifactHandler
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals
import kotlin.test.assertTrue


@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class BpmnSectionTest {

    @Autowired
    lateinit var recordsService: RecordsService

    @Autowired
    lateinit var bpmnSectionArtifactHandler: BpmnSectionArtifactHandler

    final val dataSourceId = "eproc/bpmn-section-repo"
    final val proxyDataSourceId = "eproc/bpmn-section"

    @Test
    fun bpmnSectionRepoTest() {
        val testSection = mapOf(
            "id" to "testSection1",
            "name" to MLText("testSectionName1")
        )

        val recordRef = recordsService.create(dataSourceId, testSection)
        assertEquals(recordRef.toString(), "eproc/bpmn-section-repo@testSection1")

        val atts = recordsService.getAtts(recordRef, listOf("name"))
        assertEquals(atts.getId().id, "testSection1")
        assertEquals(atts.getAtts()["name"].asText(), "testSectionName1")
        assertEquals(recordsService.delete(atts.getId()), DelStatus.OK)
    }

    @Test
    fun bpmnSectionArtifactHandlerTest() {
        val artifactData = ObjectData.create(
            mapOf(
                "id" to "testSection2",
                "name" to MLText("testSectionName2")
            )
        )

        bpmnSectionArtifactHandler.deployArtifact(artifactData)

        val name = recordsService.getAtt(
            RecordRef.valueOf("$dataSourceId@testSection2"), "name"
        ).asText()

        assertEquals(name, "testSectionName2")

        bpmnSectionArtifactHandler.deleteArtifact("testSection2")
        assertTrue { queryAll().getRecords().isEmpty() }
    }

    @Test
    fun proxySourceTest() {
        val testSection = mapOf(
            "id" to "testSection3",
            "name" to MLText("testSectionName3")
        )

        recordsService.create(dataSourceId, testSection)
        val atts = recordsService.getAtts("$proxyDataSourceId@testSection3", listOf("name"))
        assertEquals(atts.getId().toString(), "bpmn-section@testSection3")
        assertEquals(atts.getAtts()["name"].asText(), "testSectionName3")

        recordsService.delete("bpmn-section@testSection3")
        assertTrue { queryAll().getRecords().isEmpty() }
    }

    fun queryAll(): RecsQueryRes<RecordRef> {
        val query = RecordsQuery.create {
            withSourceId(dataSourceId)
            withQuery(
                Predicates.and(
                    Predicates.eq("_type", "emodel/type@bpmn-section")
                )
            )
        }
        return recordsService.query(query)
    }

}
