package ru.citeck.ecos.process.domain.cmmn.io.xml

import mu.KotlinLogging
import ru.citeck.ecos.process.domain.cmmn.model.omg.Definitions
import ru.citeck.ecos.process.domain.cmmn.model.omg.Shape
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.process.domain.procdef.convert.io.xml.XmlDefUtils
import java.io.*
import java.nio.charset.StandardCharsets
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName

object CmmnXmlUtils {

    private val log = KotlinLogging.logger {}

    const val NS_CMMN = "http://www.omg.org/spec/CMMN/20151109/MODEL"
    const val NS_ECOS = "http://www.citeck.ru/ecos/cmmn/1.0"
    const val NS_ALF_ECOS_CMMN = "http://www.citeck.ru/ecos/case/cmmn/1.0"

    val PROP_ECOS_TYPE = QName(NS_ECOS, "ecosType")
    val PROP_PROCESS_DEF_ID = QName(NS_ECOS, "processDefId")
    val PROP_NAME_ML = QName(NS_ECOS, "name_ml")

    val PROP_ECOS_CMMN_TYPE = QName(NS_ECOS, "cmmnType")

    private const val MODEL_ROOT_PACKAGE = "ru.citeck.ecos.process.domain.cmmn.model.omg"

    val schema = XmlDefUtils.loadSchema("cmmn/omg/11", listOf(
        "CMMN11.xsd",
        "CMMN11CaseModel.xsd",
        "CMMNDI11.xsd",
        "DC.xsd",
        "DI.xsd"
    ))

    fun readFromString(definition: String): Definitions {

        return try {

            val jaxbContext: JAXBContext = JAXBContext.newInstance(MODEL_ROOT_PACKAGE)
            val unmarshaller = jaxbContext.createUnmarshaller()
            unmarshaller.schema = schema
            var result: Any? = unmarshaller.unmarshal(
                ByteArrayInputStream(definition.toByteArray(StandardCharsets.UTF_8)))

            if (result is JAXBElement<*>) {
                result = result.value
            }

            result as Definitions

        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not parse stream", e)
        }
    }

    fun writeToString(definitions: Definitions): String {

        try {
            val context: JAXBContext =
                JAXBContext.newInstance(MODEL_ROOT_PACKAGE)

            val marshaller = context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

            marshaller.schema = schema

            val qName = QName(NS_CMMN, "definitions")

            val element: JAXBElement<Definitions> = JAXBElement(qName, Definitions::class.java, definitions)
            val outStream = ByteArrayOutputStream()

            marshaller.marshal(element, outStream)

            return String(outStream.toByteArray(), StandardCharsets.UTF_8)

        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not write to stream", e)
        }
    }

    @JvmStatic
    fun idRefToId(ref: Any?): String? {
        var mutRef = ref ?: return null
        if (mutRef is JAXBElement<*>) {
            mutRef = mutRef.value
        }
        if (mutRef is String) {
            return mutRef
        }
        if (mutRef is TCmmnElement) {
            return mutRef.id
        }
        if (mutRef is Shape) {
            return mutRef.id
        }
        error("Unknown type: ${mutRef::class} value: $mutRef")
    }
}
