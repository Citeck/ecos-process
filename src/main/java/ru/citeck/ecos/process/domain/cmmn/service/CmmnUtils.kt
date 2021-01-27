package ru.citeck.ecos.process.domain.cmmn.service

import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.springframework.util.DigestUtils
import org.springframework.util.ResourceUtils
import org.w3c.dom.ls.LSResourceResolver
import ru.citeck.ecos.process.domain.cmmn.model.Definitions
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.*
import javax.xml.XMLConstants
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBElement
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.namespace.QName
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

object CmmnUtils {

    private val log = KotlinLogging.logger {}

    const val NS_CMMN = "http://www.omg.org/spec/CMMN/20151109/MODEL"
    const val NS_ECOS = "http://www.citeck.ru/ecos"

    val PROP_ECOS_TYPE = QName(NS_ECOS, "ecosType")
    val PROP_PROCESS_DEF_ID = QName(NS_ECOS, "processDefId")

    private const val MODEL_ROOT_PACKAGE = "ru.citeck.ecos.process.domain.cmmn.model"

    private val schema: Schema
    private val random = Random()

    private val schemaFiles = listOf(
        "CMMN11.xsd",
        "CMMN11CaseModel.xsd",
        "CMMNDI11.xsd",
        "DC.xsd",
        "DI.xsd"
    ).map { "classpath:schema/cmmn/$it" }

    init {

        val schemaFileByName: MutableMap<String, File> = HashMap()
        val sources = arrayOfNulls<StreamSource>(schemaFiles.size)
        for (i in sources.indices) {
            val schemaFile = ResourceUtils.getFile(schemaFiles[i])
            val streamSource = StreamSource(schemaFile)
            streamSource.systemId = schemaFile.toURI().toString()
            schemaFileByName[schemaFile.name] = schemaFile
            sources[i] = streamSource
        }

        val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        schemaFactory.resourceResolver = LSResourceResolver {
                type: String?,
                namespaceURI: String?,
                publicId: String?,
                systemId: String?,
                baseURI: String? ->

            object : XmlLsInput(type, namespaceURI, publicId, systemId, baseURI) {
                override fun getXsdData(systemId: String): ByteArray? {
                    val file = schemaFileByName[systemId] ?: return null
                    try {
                        FileInputStream(file).use { fis -> return IOUtils.toByteArray(fis) }
                    } catch (e: IOException) {
                        log.error("Cannot read schema: $systemId", e)
                    }
                    return null
                }
            }
        }
        schema = schemaFactory.newSchema(sources)
    }

    fun generateId(prefix: String): String {
        val prefixValue = if (prefix.isBlank()) {
            "Id"
        } else if (!prefix[0].isLetter()) {
            error("Incorrect prefix: $prefix")
        } else {
            prefix
        }
        var time = System.nanoTime()
        val bytes = ByteArray(12)
        for (i in 7 downTo 0) {
            bytes[i] = (time and 0xFFL).toByte()
            time = time shr 8
        }
        for (i in 8 until bytes.size) {
            bytes[i] = (random.nextInt(255) - 128).toByte()
        }
        val digestId = DigestUtils.md5DigestAsHex(bytes).substring(0, 7)
        return "${prefixValue}_$digestId"
    }

    fun getSchema(): Schema {
        return schema
    }

    fun readFromString(definition: String): Definitions {

        return try {

            val jaxbContext: JAXBContext = JAXBContext.newInstance(MODEL_ROOT_PACKAGE)
            val unmarshaller = jaxbContext.createUnmarshaller()
            unmarshaller.schema = getSchema()
            var result: Any? = unmarshaller.unmarshal(
                ByteArrayInputStream(definition.toByteArray(StandardCharsets.UTF_8)))

            if (result is JAXBElement<*>) {
                result = result.value
            }

            result as Definitions

        } catch (e: JAXBException) {
            throw java.lang.IllegalArgumentException("Can not parse stream", e)
        }
    }

    fun writeToString(definitions: Definitions): String {

        try {
            val context: JAXBContext =
                JAXBContext.newInstance(MODEL_ROOT_PACKAGE)

            val marshaller = context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)

            marshaller.schema = getSchema()

            val qName = QName("http://www.omg.org/spec/CMMN/20151109/MODEL", "definitions")

            val element: JAXBElement<Definitions> = JAXBElement(qName, Definitions::class.java, definitions)
            val outStream = ByteArrayOutputStream()

            marshaller.marshal(element, outStream)

            return String(outStream.toByteArray(), StandardCharsets.UTF_8)

        } catch (e: JAXBException) {
            throw IllegalArgumentException("Can not write to stream", e)
        }
    }
}
