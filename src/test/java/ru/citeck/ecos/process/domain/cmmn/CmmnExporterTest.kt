package ru.citeck.ecos.process.domain.cmmn

import io.github.oshai.kotlinlogging.KotlinLogging
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.model.lib.ModelServiceFactory
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.*
import ru.citeck.ecos.webapp.api.entity.EntityRef

class CmmnExporterTest {

    companion object {
        val log = KotlinLogging.logger {}
    }

    val cmmnIO = CmmnIO(ModelServiceFactory().workspaceService)

    @Test
    fun fromXmlTest() {

        val procDefFile = ResourceUtils.getFile("classpath:test/cmmn/cmmn-test-process.cmmn.xml")
        val procDefXml = procDefFile.readText()

        // test
        val originalDefs = CmmnXmlUtils.readFromString(procDefXml)
        val ecosProc = cmmnIO.importEcosCmmn(originalDefs)
        println(cmmnIO.exportAlfCmmnToString(ecosProc))
        // /test

        testProc(procDefXml)
        testProc(
            cmmnIO.exportEcosCmmnToString(
                cmmnIO.generateDefaultDef("test-id", MLText(""), EntityRef.EMPTY)
            )
        )
    }

    private fun testProc(procDefXml: String) {

        val originalXmlProcess = CmmnXmlUtils.readFromString(procDefXml)
        CmmnComparator.sortAllById(originalXmlProcess)

        val ecosProcess = cmmnIO.importEcosCmmn(originalXmlProcess)
        val xmlProcessAfterExport = cmmnIO.exportEcosCmmn(ecosProcess)

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

        val ecosProcess2 = cmmnIO.importEcosCmmn(xmlProcessAfterExport)

        assertEquals(
            Json.mapper.convert(ecosProcess, ObjectData::class.java),
            Json.mapper.convert(ecosProcess2, ObjectData::class.java)
        )
    }
}
