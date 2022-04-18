package ru.citeck.ecos.process.domain.bpmn.io.convert

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaField
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaString
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.BoundsDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.PointDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.omg.Bounds
import ru.citeck.ecos.process.domain.bpmn.model.omg.Point
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.records2.RecordRef
import javax.xml.bind.JAXBElement

fun Bounds.toDef(): BoundsDef {
    return BoundsDef(
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height
    )
}

fun BoundsDef.toOmg(): Bounds {
    val bounds = Bounds()

    bounds.x = x
    bounds.y = y
    bounds.width = width
    bounds.height = height

    return bounds
}

fun Point.toDef(): PointDef {
    return PointDef(
        x = this.x,
        y = this.y
    )
}

fun PointDef.toOmg(): Point {
    val point = Point()

    point.x = x
    point.y = y

    return point
}

object CamundaFieldCreator {

    fun string(name: String, value: String): CamundaField {
        val stringValue = CamundaString().apply {
            this.value = value
        }

        return CamundaField().apply {
            this.name = name
            this.stringValue = stringValue
        }
    }

}

fun CamundaField.jaxb(context: ExportContext): JAXBElement<CamundaField> {
    return context.converters.convertToJaxb(this)
}

inline fun <K, reified V> MutableMap<in K, in V>.putIfNotBlank(key: K, value: V) {
    when (true) {
        value is String -> {
            if (value.isNotBlank()) put(key, value)
        }
        value is RecordRef -> {
            if (RecordRef.isNotEmpty(value)) put(key, value)
        }
        else -> error("Type ${V::class} is not supported")
    }
}

inline fun <reified T> MutableList<in T>.addIfNotBlank(value: T) {
    when (true) {
        value is String -> {
            if (value.isNotBlank() && value != "null") add(value)
        }
        value is RecordRef -> {
            if (RecordRef.isNotEmpty(value)) add(value)
        }
        value is CamundaField -> {
            if (value.stringValue?.value?.isNotBlank() == true) add(value)
        }
        else -> error("Type ${T::class} is not supported")
    }
}

fun recipientsFromJson(type: RecipientType, jsonData: String): List<Recipient> {
    if (jsonData.isBlank()) {
        return emptyList()
    }

    return Json.mapper.readList(jsonData, String::class.java).map {
        Recipient(type, it)
    }
}

fun recipientsFromJson(jsonData: String): List<Recipient> {
    if (jsonData.isBlank()) {
        return emptyList()
    }

    return Json.mapper.readList(jsonData, Recipient::class.java)
}

fun recipientsToJsonWithoutType(recipients: List<Recipient>): String {
    val values = recipients.map { it.value }
    return Json.mapper.toString(values) ?: ""
}

fun recipientsToJson(recipients: List<Recipient>): String {
    return Json.mapper.toString(recipients) ?: ""
}
