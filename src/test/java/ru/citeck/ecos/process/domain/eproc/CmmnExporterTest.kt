package ru.citeck.ecos.process.domain.eproc

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsServiceFactory
import ru.citeck.ecos.records3.record.request.RequestContext

class CmmnExporterTest {

    companion object {
        val log = KotlinLogging.logger {}
    }

    @Test
    fun fromXmlTest() {

        val procDefFile = ResourceUtils.getFile("classpath:test/cmmn/cmmn-test-process.cmmn.xml")
        val procDefXml = procDefFile.readText()

        RequestContext.doWithCtx(RecordsServiceFactory()) {
            testProc(procDefXml)
            testProc(CmmnIO.exportToString(
                CmmnIO.generateDefaultDef("test-id", MLText(""), RecordRef.EMPTY)
            ))
        }
    }

    private fun testProc(procDefXml: String) {

        val originalXmlProcess = CmmnXmlUtils.readFromString(procDefXml)
        CmmnComparator.sortAllById(originalXmlProcess)

        val ecosProcess = CmmnIO.import(originalXmlProcess)
        val xmlProcessAfterExport = CmmnIO.export(ecosProcess)

        CmmnComparator.sortAllById(xmlProcessAfterExport)

        val excludedProps = mapOf(
            Definitions::class.java to setOf("exporter", "exporterVersion"),
            TUserEventListener::class.java to setOf("authorizedRoleRefs"),
            Case::class.java to setOf("caseRoles")
        )
        val idRefsProps: Map<Class<*>, Set<String>> = mapOf(
            TAssociation::class.java to setOf("sourceRef", "targetRef"),
            TCriterion::class.java to setOf("sentryRef"),
            TPlanItem::class.java to setOf("definitionRef"),
            TPlanItemOnPart::class.java to setOf("sourceRef", "exitCriterionRef"),
            TCaseFileItemOnPart::class.java to setOf("sourceRef")
        )
        val compareResult = CmmnComparator.compare(
            originalXmlProcess,
            xmlProcessAfterExport,
            excludedProps,
            idRefsProps
        )
        if (!compareResult) {
            log.error { "Before: \n${CmmnXmlUtils.writeToString(originalXmlProcess)}" }
            log.error { "After: \n${CmmnXmlUtils.writeToString(xmlProcessAfterExport)}" }
        }

        assertTrue(compareResult)

        val ecosProcess2 = CmmnIO.import(xmlProcessAfterExport)

        assertEquals(
            Json.mapper.convert(ecosProcess, ObjectData::class.java),
            Json.mapper.convert(ecosProcess2, ObjectData::class.java)
        )
    }
}
