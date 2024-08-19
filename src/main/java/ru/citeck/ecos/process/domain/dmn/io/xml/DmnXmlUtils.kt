package ru.citeck.ecos.process.domain.dmn.io.xml

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBElement
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import ru.citeck.ecos.process.domain.dmn.io.NS_DMN
import ru.citeck.ecos.process.domain.dmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.procdef.convert.io.xml.XmlDefUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import javax.xml.namespace.QName

object DmnXmlUtils {

    private const val MODEL_ROOT_PACKAGES = "ru.citeck.ecos.process.domain.dmn.model.omg"

    private val schema = XmlDefUtils.loadSchema(
        "dmn/omg/13",
        listOf(
            "DMN13.xsd",
            "DC.xsd",
            "DI.xsd",
            "DMNDI13.xsd"
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

            val qName = QName(NS_DMN, "definitions")

            val element: JAXBElement<TDefinitions> = JAXBElement(qName, TDefinitions::class.java, definitions)
            val outStream = ByteArrayOutputStream()

            marshaller.marshal(element, outStream)

            return removeEmptyXmlns(String(outStream.toByteArray(), StandardCharsets.UTF_8))
        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not write to stream", e)
        }
    }

    //  Dmn modeler failed to parse definition with empty xmlns.
    // I didn’t find a way to get rid of it when converting, so we use the replacement hack
    private fun removeEmptyXmlns(data: String): String {
        return data.replace("xmlns=\"\"", "")
    }
}
