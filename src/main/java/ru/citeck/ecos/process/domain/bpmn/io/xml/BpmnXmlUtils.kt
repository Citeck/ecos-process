package ru.citeck.ecos.process.domain.bpmn.io.xml

import ru.citeck.ecos.process.domain.bpmn.io.NS_BPMN
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

    private const val MODEL_ROOT_PACKAGES = "ru.citeck.ecos.process.domain.bpmn.model.omg" +
        ":ru.citeck.ecos.process.domain.bpmn.model.camunda"

    private val schema = XmlDefUtils.loadSchema(
        "bpmn/omg/20",
        listOf(
            "BPMN20.xsd",
            "Semantic.xsd",
            "DC.xsd",
            "DI.xsd",
            "BPMNDI.xsd"
        )
    )

    fun readFromString(definition: String): TDefinitions {

        return try {

            val jaxbContext: JAXBContext = JAXBContext.newInstance(MODEL_ROOT_PACKAGES)

            val unmarshaller = jaxbContext.createUnmarshaller()
            unmarshaller.schema = schema

            var result: Any? = unmarshaller.unmarshal(
                ByteArrayInputStream(definition.toByteArray(StandardCharsets.UTF_8))
            )

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
            val context: JAXBContext = JAXBContext.newInstance(MODEL_ROOT_PACKAGES)

            val marshaller = context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
            marshaller.schema = schema

            val qName = QName(NS_BPMN, "definitions")

            val element: JAXBElement<TDefinitions> = JAXBElement(qName, TDefinitions::class.java, definitions)
            val outStream = ByteArrayOutputStream()

            marshaller.marshal(element, outStream)

            return removeEmptyXmlns(String(outStream.toByteArray(), StandardCharsets.UTF_8));
        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not write to stream", e)
        }
    }

    //  Bpmn modeler failed to parse definition with empty xmlns.
    // I didn’t find a way to get rid of it when converting, so we use the replacement hack
    private fun removeEmptyXmlns(data: String): String {
        return data.replace("xmlns=\"\"", "")
    }
}
