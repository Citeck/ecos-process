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
        val recordsBefore = queryAll().getRecords().size

        val testSection = mapOf(
            "id" to "testSection1",
            "name" to MLText("testSectionName1")
        )

        val testSubSection = mapOf(
            "id" to "testSubSection1",
            "name" to MLText("testSubSectionName1"),
            "parentId" to "testSection1"
        )

        val recordRef = recordsService.create(dataSourceId, testSection)
        assertEquals(recordRef.toString(), "eproc/bpmn-section-repo@testSection1")

        val atts = recordsService.getAtts(recordRef, listOf("name"))
        assertEquals(atts.getId().id, "testSection1")
        assertEquals(atts.getAtts()["name"].asText(), "testSectionName1")

        val recordRef2 = recordsService.create(dataSourceId, testSubSection)
        assertEquals(recordRef2.toString(), "eproc/bpmn-section-repo@testSubSection1")

        assertEquals(recordsService.delete(recordRef), DelStatus.OK)
        assertEquals(recordsService.delete(recordRef2), DelStatus.OK)

        assertEquals(queryAll().getRecords().size, recordsBefore)
    }

    @Test
    fun bpmnSectionArtifactHandlerTest() {
        val recordsBefore = queryAll().getRecords().size
        val artifactData = ObjectData.create(
            mapOf(
                "id" to "testSection2",
                "name" to MLText("testSectionName2")
            )
        )

        val artifactData2 = ObjectData.create(
            mapOf(
                "id" to "testSection2-1",
                "name" to MLText("testSectionName2-1"),
                "parentId" to "testSection2"
            )
        )

        bpmnSectionArtifactHandler.deployArtifact(artifactData)
        bpmnSectionArtifactHandler.deployArtifact(artifactData2)

        val name = recordsService.getAtt(
            RecordRef.valueOf("$dataSourceId@testSection2"), "name"
        ).asText()
        assertEquals(name, "testSectionName2")

        val parentOfRec2 = recordsService.getAtt(
            RecordRef.valueOf("$dataSourceId@testSection2-1"), "parentId"
        ).asText()
        assertEquals(parentOfRec2, "testSection2")


        bpmnSectionArtifactHandler.deleteArtifact("testSection2")
        bpmnSectionArtifactHandler.deleteArtifact("testSection2-1")
        assertEquals(queryAll().getRecords().size, recordsBefore)
    }

    @Test
    fun proxySourceTest() {
        val recordsBefore = queryAll().getRecords().size
        val testSection = mapOf(
            "id" to "testSection3",
            "name" to MLText("testSectionName3")
        )

        recordsService.create(proxyDataSourceId, testSection)
        val atts = recordsService.getAtts("$proxyDataSourceId@testSection3", listOf("name"))
        assertEquals(atts.getId().toString(), "bpmn-section@testSection3")
        assertEquals(atts.getAtts()["name"].asText(), "testSectionName3")

        recordsService.delete("bpmn-section@testSection3")
        assertEquals(queryAll().getRecords().size, recordsBefore)
    }

    @Test
    fun proxySourceMutateTest() {
        val recordsBefore = queryAll().getRecords().size
        val testSection = mapOf(
            "id" to "testSection4",
            "name" to MLText("testSectionName4")
        )

        val changeNameAtt = mapOf(
            "name" to MLText("Renamed")
        )

        recordsService.create(proxyDataSourceId, testSection)
        recordsService.mutate("$proxyDataSourceId@testSection4", changeNameAtt)

        val atts = recordsService.getAtts("$proxyDataSourceId@testSection4", listOf("name"))
        assertEquals(atts.getId().toString(), "bpmn-section@testSection4")
        assertEquals(atts.getAtts()["name"].asText(), "Renamed")

        recordsService.delete("bpmn-section@testSection4")
        assertEquals(queryAll().getRecords().size, recordsBefore)
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
