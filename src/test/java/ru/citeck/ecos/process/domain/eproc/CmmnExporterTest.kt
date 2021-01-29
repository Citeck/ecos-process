package ru.citeck.ecos.process.domain.eproc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.cmmn.io.CmmnIO
import ru.citeck.ecos.records2.RecordRef

class CmmnExporterTest {

    @Test
    fun fromXmlTest() {

        val procDefFile = ResourceUtils.getFile("classpath:test/cmmn/cmmn-test-process.cmmn.xml")
        val procDefXml = procDefFile.readText()

        testProc(procDefXml)
        testProc(CmmnIO.exportToString(
            CmmnIO.generateDefaultDef("test-id", MLText(""), RecordRef.EMPTY)
        ))
    }

    private fun testProc(procDefXml: String) {

        val cmmnProcess = CmmnIO.import(procDefXml)
        val cmmnXmlProc = CmmnIO.exportToString(cmmnProcess)
        val cmmnProcess2 = CmmnIO.import(cmmnXmlProc)
        val cmmnXmlProc2 = CmmnIO.exportToString(cmmnProcess2)

        assertEquals(
            Json.mapper.convert(cmmnProcess, ObjectData::class.java),
            Json.mapper.convert(cmmnProcess2, ObjectData::class.java)
        )
        assertEquals(cmmnXmlProc, cmmnXmlProc2)
    }

/*    @Test
    fun ecosCmmnTest() {
        val procDef = CmmnPlanModelDef(
            id = "abcd",
            name = "Test Case",
            children = listOf(
                CmmnActivityDef("innerStage", "inner stageName", true,
                    emptyList(),
                    emptyList(),
                    CmmnActivityType.STAGE,
                    Json.mapper.convert(CmmnStage(
                        true,
                        listOf(
                            CmmnActivityDef("innerProcTask", "inner procTask", false,
                                listOf(
                                    CmmnSentryDef(
                                        "Sentry_123",
                                        listOf(CmmnOnPartDef("innerProcTask22", "complete", null)),
                                        null
                                    )
                                ),
                                emptyList(),
                                CmmnActivityType.PROCESS_TASK,
                                ObjectData.create("""{
                                    "processDefId": "confirm",
                                    "processType": "flowable-bpmn"
                                }""".trimIndent())
                            ),
                            CmmnActivityDef("innerProcTask22", "inner procTask22", false,
                                emptyList(),
                                emptyList(),
                                CmmnActivityType.PROCESS_TASK,
                                ObjectData.create("""{
                                    "processDefId": "confirm",
                                    "processType": "flowable-bpmn"
                                }""".trimIndent())
                            )
                        )
                    ), ObjectData::class.java)!!
                )
            ),
            exitEvents = emptyList()
        )

        val textOut = cmmnExporter.export(procDef)

        println(textOut)

        val parsedData = cmmnToEcosCmmnConverter.convert(textOut)

        print(Json.mapper.toPrettyString(parsedData))
    }*/
}
