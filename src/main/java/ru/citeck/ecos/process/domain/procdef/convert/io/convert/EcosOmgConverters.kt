package ru.citeck.ecos.process.domain.procdef.convert.io.convert

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.commons.utils.ReflectUtils
import ru.citeck.ecos.process.domain.cmmn.model.omg.ObjectFactory
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.namespace.QName
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredFunctions

class EcosOmgConverters(
    val base: EcosOmgConverters?,
    converters: List<KClass<out EcosOmgConverter<*, *>>>,
    private val typeResolver: (Any) -> String? = { null },
    private val otherAttsResolver: (Any) -> MutableMap<QName, String>? = { null }
) {

    constructor(
        converters: List<KClass<out EcosOmgConverter<*, *>>>,
        typeResolver: (Any) -> String? = { null },
        otherAttsResolver: (Any) -> MutableMap<QName, String>? = { null }
    ) : this(null, converters, typeResolver, otherAttsResolver)

    private val convertersByType: Map<String, ConverterInfo>
    private val convertersByOmgType: Map<Class<*>, ConverterInfo>
    private val jaxbCreateMethods: MutableMap<Class<*>, (Any) -> JAXBElement<Any>> = mutableMapOf()

    private val omgObjectFactory = ru.citeck.ecos.process.domain.bpmn.model.omg.ObjectFactory()

    private val jaxbFactories = listOf(
        ObjectFactory(),
        omgObjectFactory,
        ru.citeck.ecos.process.domain.bpmn.model.camunda.ObjectFactory()
    )

    init {

        val convertersList = converters.map {
            it.createInstance()
        }.map {
            @Suppress("UNCHECKED_CAST")
            val converter = it as EcosOmgConverter<Any, Any>
            val args = ReflectUtils.getGenericArgs(it::class.java, EcosOmgConverter::class.java)
            ConverterInfo(args[0], args[1], converter)
        }

        convertersByType = convertersList.map {
            ConvertUtils.getTypeByClass(it.ecosType) to it
        }.toMap()
        convertersByOmgType = convertersList.filter {
            !ConvertUtils.getTypeByClass(it.ecosType).startsWith("ecos:")
        }.associateBy { it.omgType }

        jaxbFactories.forEach { initJaxbCreateMethods(it) }
    }

    private fun initJaxbCreateMethods(factory: Any) {
        val methods = factory::class.declaredFunctions.filter {
            if (it.parameters.size != 2) {
                return@filter false
            }
            val clazz = it.parameters[1].type.classifier
            if (clazz !is KClass<*>) {
                return@filter false
            }
            it.returnType.classifier == JAXBElement::class && it.name.startsWith("create")
        }.associate {
            val func: (Any) -> JAXBElement<Any> = { item ->
                @Suppress("UNCHECKED_CAST")
                it.call(factory, item) as JAXBElement<Any>
            }
            (it.parameters[1].type.classifier as KClass<*>).java to func
        }

        jaxbCreateMethods.putAll(methods)
    }

    fun <T : Any> export(element: Any): T {
        return export(element, ExportContext(this))
    }

    fun <T : Any> export(element: Any, context: ExportContext): T {
        @Suppress("UNCHECKED_CAST")
        return export(ConvertUtils.getTypeByClass(element::class.java), element, context)
    }

    fun <T : Any> export(element: Any, expectedType: Class<T>, context: ExportContext): T {
        @Suppress("UNCHECKED_CAST")
        return export(ConvertUtils.getTypeByClass(element::class.java), element, context)
    }

    fun <T : Any> export(type: String, element: Any, context: ExportContext): T {

        val converter = convertersByType[type] ?: base?.convertersByType?.get(type)
            ?: error("Type is not registered: $type")

        val convertedElement = Json.mapper.convert(element, converter.ecosType)
            ?: error("Conversion failed for $element of type $type and class ${converter.ecosType}")

        @Suppress("UNCHECKED_CAST")
        val result = converter.converter.export(convertedElement, context) as T
        val otherAtts = otherAttsResolver.invoke(result) ?: base?.otherAttsResolver?.invoke(result)
        if (otherAtts != null) {
            val emptyProps = otherAtts.keys.filter {
                val value = otherAtts[it]
                value == null || value == ""
            }
            emptyProps.forEach { otherAtts.remove(it) }
        }
        return result
    }

    fun <T : Any> convertToJaxb(item: T): JAXBElement<T> {

        val jaxbCreate = jaxbCreateMethods[item::class.java]
            ?: error("Jaxb create method is not found for ${item::class.java}")

        @Suppress("UNCHECKED_CAST")
        return jaxbCreate.invoke(item) as JAXBElement<T>
    }

    fun <T : Any> convertToJaxbFlowNodeRef(item: T): JAXBElement<T> {
        @Suppress("UNCHECKED_CAST")
        return omgObjectFactory.createTLaneFlowNodeRef(item) as JAXBElement<T>
    }

    fun import(item: Any, validate: Boolean = true): EcosElementData<ObjectData> {
        return import(item, ImportContext(this, validate))
    }

    fun import(item: Any, context: ImportContext): EcosElementData<ObjectData> {
        return import(item, ObjectData::class.java, context)
    }

    fun <T : Any> import(item: Any, expectedType: Class<T>, validate: Boolean = true): EcosElementData<T> {
        return import(item, expectedType, ImportContext(this, validate))
    }

    fun <T : Any> import(item: Any, expectedType: Class<T>, context: ImportContext): EcosElementData<T> {

        val extensionType = typeResolver.invoke(item)

        val converter = if (!extensionType.isNullOrBlank()) {
            convertersByType[extensionType] ?: base?.convertersByType?.get(extensionType)
                ?: error("Type is not registered: $extensionType")
        } else {
            val type: Class<*> = item::class.java
            getConverterByOmgType(type) ?: base?.getConverterByOmgType(type)
                ?: error("Type is not registered: ${item::class.java}")
        }

        val ecosData = converter.converter.import(item, context)
        if (context.validateRequired && ecosData is Validated) {
            ecosData.validate()
        }

        val type = ConvertUtils.getTypeByClass(ecosData::class.java)

        val convertedData = Json.mapper.convert(ecosData, expectedType)
            ?: error("Conversion error. Value: $ecosData Target Type: $expectedType")

        return EcosElementData(type, convertedData)
    }

    private fun getConverterByOmgType(type: Class<*>?): ConverterInfo? {

        var converter: ConverterInfo? = null
        var typeVar = type
        while (converter == null && typeVar != null) {
            converter = convertersByOmgType[typeVar]
            typeVar = typeVar.superclass
        }
        return converter
    }

    private class ConverterInfo(
        val ecosType: Class<*>,
        val omgType: Class<*>,
        val converter: EcosOmgConverter<Any, Any>
    )
}
