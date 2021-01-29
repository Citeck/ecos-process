package ru.citeck.ecos.process.domain.cmmn.io.convert

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.DiagramElement
import ru.citeck.ecos.process.domain.cmmn.model.omg.ObjectFactory
import ru.citeck.ecos.process.domain.cmmn.model.omg.TCmmnElement
import ru.citeck.ecos.process.domain.cmmn.io.xml.CmmnXmlUtils
import ru.citeck.ecos.process.domain.cmmn.io.convert.artifact.AssociationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.artifact.TextAnnotationConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.di.CmmnDiConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.di.EdgeConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.di.ShapeConverter
import ru.citeck.ecos.process.domain.cmmn.io.context.ExportContext
import ru.citeck.ecos.process.domain.cmmn.io.context.ImportContext
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.*
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event.EntryCriterionConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event.ExitCriterionConverter
import ru.citeck.ecos.process.domain.cmmn.io.convert.plan.event.SentryConverter
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredFunctions

class CmmnConverters(
    objectFactory: ObjectFactory
) {

    companion object {
        private val PROP_TYPE = QName(CmmnXmlUtils.NS_ECOS, "cmmnType")
    }

    private val convertersByActivityType: Map<String, ConverterInfo>
    private val convertersByOmgType: Map<Class<*>, ConverterInfo>
    private val jaxbCreateMethods: Map<Class<*>, (Any) -> JAXBElement<Any>>

    init {

        val convertersList = listOf(
            DefinitionsConverter(this),
            CmmnDiConverter(this),
            ActivityConverter(this),
            DefinitionsConverter(this),
            SentryConverter(this),
            StageConverter(this),
            EdgeConverter(),
            ShapeConverter(),
            HumanTaskConverter(),
            ProcessTaskConverter(),
            TimerEventListenerConverter(),
            UserEventListenerConverter(),
            AssociationConverter(),
            TextAnnotationConverter(),
            ExitCriterionConverter(this),
            EntryCriterionConverter(this)
        ).map {
            @Suppress("UNCHECKED_CAST")
            val converter = it as CmmnConverter<Any, Any>
            val args = ReflectUtils.getGenericArgs(it::class.java, CmmnConverter::class.java)
            ConverterInfo(args[0], args[1], converter)
        }

        convertersByActivityType = convertersList.map { it.converter.getElementType() to it }.toMap()
        convertersByOmgType = convertersList.filter {
            !it.converter.isExtensionType()
        }.map { it.omgType to it }.toMap()

        jaxbCreateMethods = objectFactory::class.declaredFunctions.filter {
            if (it.parameters.size != 2) {
                return@filter false
            }
            val clazz = it.parameters[1].type.classifier
            if (clazz !is KClass<*>) {
                return@filter false
            }
            it.returnType.classifier == JAXBElement::class && it.name.startsWith("create")
        }.map {
            val func: (Any) -> JAXBElement<Any> = { item ->
                @Suppress("UNCHECKED_CAST")
                it.call(objectFactory, item) as JAXBElement<Any>
            }
            (it.parameters[1].type.classifier as KClass<*>).java to func
        }.toMap()
    }

    fun <T : Any> export(type: String, element: Any, context: ExportContext): T {

        val converter = convertersByActivityType[type] ?: error("Type is not registered: $type")
        val cmmnElement = Json.mapper.convert(element, converter.cmmnType)
            ?: error("Conversion failed for $element of type $type and class ${converter.cmmnType}")

        @Suppress("UNCHECKED_CAST")
        return converter.converter.export(cmmnElement, context) as T
    }

    fun <T : Any> convertToJaxb(item: T): JAXBElement<T> {

        val jaxbCreate = jaxbCreateMethods[item::class.java]
                ?: error("Jaxb create method is not found for ${item::class.java}")

        @Suppress("UNCHECKED_CAST")
        return jaxbCreate.invoke(item) as JAXBElement<T>
    }

    fun import(item: Any, context: ImportContext): CmmnElementData<ObjectData> {
        return import(item, ObjectData::class.java, context)
    }

    fun <T: Any> import(item: Any, expectedType: Class<T>, context: ImportContext): CmmnElementData<T> {

        val extensionType = when (item) {
            is DiagramElement -> item.otherAttributes[PROP_TYPE]
            is TCmmnElement -> item.otherAttributes[PROP_TYPE]
            else -> null
        }
        val converter = if (!extensionType.isNullOrBlank()) {
            convertersByActivityType[extensionType] ?: error("Type is not registered: $extensionType")
        } else {
            var type: Class<*>? = item::class.java
            var converter: ConverterInfo? = null
            while (type != null) {
                converter = convertersByOmgType[item::class.java]
                type = type.superclass
            }
            converter ?: error("Type is not registered: ${item::class.java}")
        }

        val ecosCmmnData = converter.converter.import(item, context)

        val convertedData = Json.mapper.convert(ecosCmmnData, expectedType)
                ?: error("Conversion error. Value: $ecosCmmnData Target Type: $expectedType")

        return CmmnElementData(converter.converter.getElementType(), convertedData)
    }

    private class ConverterInfo(
        val omgType: Class<*>,
        val cmmnType: Class<*>,
        val converter: CmmnConverter<Any, Any>
    )
}
