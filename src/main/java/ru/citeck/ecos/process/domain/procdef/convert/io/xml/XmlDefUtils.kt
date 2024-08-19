package ru.citeck.ecos.process.domain.procdef.convert.io.xml

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.xml.bind.JAXBElement
import org.apache.commons.io.IOUtils
import org.springframework.util.DigestUtils
import org.springframework.util.ResourceUtils
import org.w3c.dom.ls.LSResourceResolver
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.*
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema
import javax.xml.validation.SchemaFactory

object XmlDefUtils {

    private val log = KotlinLogging.logger {}

    private const val SCHEMA_ROOT = "classpath:schema/"

    private val random = Random()

    fun loadSchema(path: String, files: List<String>): Schema {

        val schemaRoot = ResourceUtils.getFile(SCHEMA_ROOT + path)
        if (!schemaRoot.exists()) {
            error("Schema path is not found: $path")
        }
        if (!schemaRoot.isDirectory) {
            error("Schema path should be a directory: $path")
        }
        val schemaFiles = files.map { File(schemaRoot, it) }

        val schemaFileByName: MutableMap<String, File> = HashMap()
        val sources = arrayOfNulls<StreamSource>(schemaFiles.size)
        for (i in sources.indices) {
            val schemaFile = schemaFiles[i]
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

                override fun getXsdData(systemId: String): ByteArray {
                    val file = schemaFileByName[systemId] ?: error("Schema is not found: $systemId")
                    return try {
                        FileInputStream(file).use { IOUtils.toByteArray(it) }
                    } catch (e: IOException) {
                        throw RuntimeException("Cannot read schema: $systemId", e)
                    }
                }
            }
        }
        return schemaFactory.newSchema(sources)
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

    @JvmStatic
    fun <T : Any> unwrapJaxb(value: JAXBElement<T>?): T? {
        return value?.value
    }

    @JvmStatic
    fun <T : Any> unwrapJaxb(list: List<JAXBElement<*>>?): List<T>? {
        @Suppress("UNCHECKED_CAST")
        return list?.map { it.value as T }
    }
}
