package ru.citeck.ecos.process.domain.bpmn.io.xml

import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.xml.XmlDefUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName

object BpmnXmlUtils {

    private const val MODEL_ROOT_PACKAGE = "ru.citeck.ecos.process.domain.bpmn.model.omg"

    const val NS_BPMN = "http://www.omg.org/spec/BPMN/20100524/MODEL"
    const val NS_ECOS = "http://www.citeck.ru/ecos/bpmn/1.0"

    val PROP_ECOS_TYPE = QName(NS_ECOS, "ecosType")
    val PROP_PROCESS_DEF_ID = QName(NS_ECOS, "processDefId")
    val PROP_NAME_ML = QName(NS_ECOS, "name_ml")

    val PROP_ECOS_BPMN_TYPE = QName(NS_ECOS, "bpmnType")

    val schema = XmlDefUtils.loadSchema("bpmn/omg/20", listOf(
        "BPMN20.xsd",
        "Semantic.xsd",
        "DC.xsd",
        "DI.xsd",
        "BPMNDI.xsd"
    ))

    fun readFromString(definition: String): TDefinitions {

        return try {

            val jaxbContext: JAXBContext = JAXBContext.newInstance(MODEL_ROOT_PACKAGE)
            val unmarshaller = jaxbContext.createUnmarshaller()
            unmarshaller.schema = schema
            var result: Any? = unmarshaller.unmarshal(
                ByteArrayInputStream(definition.toByteArray(StandardCharsets.UTF_8)))

            if (result is JAXBElement<*>) {
                result = result.value
            }

            result as TDefinitions

        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not parse stream", e)
        }
    }

    fun writeToString(definitions: TDefinitions): String {

        try {
            val context: JAXBContext = JAXBContext.newInstance(MODEL_ROOT_PACKAGE)

            val marshaller = context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

            marshaller.schema = schema

            val qName = QName(NS_BPMN, "definitions")

            val element: JAXBElement<TDefinitions> = JAXBElement(qName, TDefinitions::class.java, definitions)
            val outStream = ByteArrayOutputStream()

            marshaller.marshal(element, outStream)

            return String(outStream.toByteArray(), StandardCharsets.UTF_8)

        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not write to stream", e)
        }
    }
}
