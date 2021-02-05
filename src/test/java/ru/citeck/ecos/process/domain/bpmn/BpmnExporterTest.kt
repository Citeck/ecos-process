package ru.citeck.ecos.process.domain.bpmn

import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.process.domain.bpmn.io.BpmnIO
import ru.citeck.ecos.process.domain.bpmn.io.xml.BpmnXmlUtils

class BpmnExporterTest {

    @Test
    fun test() {

        val procDefFile = ResourceUtils.getFile("classpath:test/bpmn/bpmn-test.bpmn.xml")
        val procDefXml = procDefFile.readText()

        val defXml = BpmnXmlUtils.readFromString(procDefXml)

        //val bpmnDef = BpmnIO.importEcosBpmn(defXml)

        //println(bpmnDef)
        //println(defXml)
    }
}
