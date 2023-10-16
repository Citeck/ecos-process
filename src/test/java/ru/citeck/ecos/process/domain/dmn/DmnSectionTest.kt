package ru.citeck.ecos.process.domain.dmn

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.dmnsection.config.DMN_SECTIONS_RECORDS_ID
import ru.citeck.ecos.process.domain.dmnsection.config.DMN_SECTION_REPO_SOURCE_ID
import ru.citeck.ecos.process.domain.dmnsection.eapps.DmnSectionArtifactHandler
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.lib.spring.test.extension.EcosSpringExtension
import kotlin.test.assertEquals

@ExtendWith(EcosSpringExtension::class)
@SpringBootTest(classes = [EprocApp::class])
class DmnSectionTest {

    companion object {
        private const val REPO_DATA_SOURCE_ID = AppName.EPROC + "/" + DMN_SECTION_REPO_SOURCE_ID
        private const val PROXY_DATA_SOURCE_ID = AppName.EPROC + "/" + DMN_SECTIONS_RECORDS_ID
    }

    @Autowired
    lateinit var recordsService: RecordsService

    @Autowired
    lateinit var dmnSectionArtifactHandler: DmnSectionArtifactHandler

    @Test
    fun dmnSectionRepoTest() {

        val recordsBefore = queryAll().getRecords().size

        val testSection = mapOf(
            "id" to "testSection1",
            "name" to MLText("testSectionName1")
        )

        val testSubSection = mapOf(
            "id" to "testSubSection1",
            "name" to MLText("testSubSectionName1"),
            "parentRef" to "eproc/dmn-section@testSection1"
        )

        val recordRef = recordsService.create(REPO_DATA_SOURCE_ID, testSection)
        assertEquals("eproc/dmn-section-repo@testSection1", recordRef.toString())

        val atts = recordsService.getAtts(recordRef, listOf("name"))
        assertEquals("testSection1", atts.getId().id)
        assertEquals("testSectionName1", atts.getAtts()["name"].asText())

        val recordRef2 = recordsService.create(REPO_DATA_SOURCE_ID, testSubSection)
        assertEquals("eproc/dmn-section-repo@testSubSection1", recordRef2.toString())

        assertEquals(recordsService.delete(recordRef), DelStatus.OK)
        assertEquals(recordsService.delete(recordRef2), DelStatus.OK)

        assertEquals(queryAll().getRecords().size, recordsBefore)
    }

    @Test
    fun dmnSectionArtifactHandlerTest() {

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
                "parentRef" to "eproc/dmn-section@testSection2"
            )
        )

        dmnSectionArtifactHandler.deployArtifact(artifactData)
        dmnSectionArtifactHandler.deployArtifact(artifactData2)

        val name = recordsService.getAtt(
            RecordRef.valueOf("$REPO_DATA_SOURCE_ID@testSection2"),
            "name"
        ).asText()
        assertEquals(name, "testSectionName2")

        val parentOfRec2 = recordsService.getAtt(
            RecordRef.valueOf("$REPO_DATA_SOURCE_ID@testSection2-1"),
            "parentRef?id"
        ).asText()
        assertEquals(parentOfRec2, "eproc/dmn-section@testSection2")

        dmnSectionArtifactHandler.deleteArtifact("testSection2")
        dmnSectionArtifactHandler.deleteArtifact("testSection2-1")
        assertEquals(queryAll().getRecords().size, recordsBefore)
    }

    @Test
    fun proxySourceTest() {

        val recordsBefore = queryAll().getRecords().size
        val testSection = mapOf(
            "id" to "testSection3",
            "name" to MLText("testSectionName3")
        )

        recordsService.create(PROXY_DATA_SOURCE_ID, testSection)
        val atts = recordsService.getAtts("$PROXY_DATA_SOURCE_ID@testSection3", listOf("name"))
        assertEquals("eproc/dmn-section@testSection3", atts.getId().toString())
        assertEquals("testSectionName3", atts.getAtts()["name"].asText())

        recordsService.delete("dmn-section@testSection3")
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

        recordsService.create(PROXY_DATA_SOURCE_ID, testSection)
        recordsService.mutate("$PROXY_DATA_SOURCE_ID@testSection4", changeNameAtt)

        val atts = recordsService.getAtts("$PROXY_DATA_SOURCE_ID@testSection4", listOf("name"))
        assertEquals("eproc/dmn-section@testSection4", atts.getId().toString())
        assertEquals("Renamed", atts.getAtts()["name"].asText())

        recordsService.delete("dmn-section@testSection4")
        assertEquals(queryAll().getRecords().size, recordsBefore)
    }

    fun queryAll(): RecsQueryRes<RecordRef> {
        val query = RecordsQuery.create {
            withSourceId(REPO_DATA_SOURCE_ID)
            withQuery(
                Predicates.and(
                    Predicates.eq("_type", "emodel/type@dmn-section")
                )
            )
        }
        return recordsService.query(query)
    }
}
