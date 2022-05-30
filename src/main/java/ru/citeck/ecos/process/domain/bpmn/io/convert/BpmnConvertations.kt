package ru.citeck.ecos.process.domain.bpmn.io.convert

import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_SCRIPT_ENGINE_LANGUAGE
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaFailedJobRetryTimeCycle
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaField
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaString
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.BoundsDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.PointDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.BpmnConditionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome.Companion.OUTCOME_VAR
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.script.BpmnScriptTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.Bounds
import ru.citeck.ecos.process.domain.bpmn.model.omg.Point
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExpression
import ru.citeck.ecos.process.domain.bpmn.model.omg.TScript
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.records2.RecordRef
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

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

fun CamundaFailedJobRetryTimeCycle.jaxb(context: ExportContext): JAXBElement<CamundaFailedJobRetryTimeCycle> {
    return context.converters.convertToJaxb(this)
}

inline fun <K, reified V> MutableMap<in K, in V>.putIfNotBlank(key: K, value: V) {
    when (value) {
        is String -> {
            if (value.isNotBlank() && value != "null") put(key, value)
        }
        is RecordRef -> {
            if (RecordRef.isNotEmpty(value)) put(key, value)
        }
        else -> error("Type ${V::class} is not supported")
    }
}

inline fun <reified T> MutableList<in T>.addIfNotBlank(value: T) {
    when (value) {
        is String -> {
            if (value.isNotBlank() && value != "null") add(value)
        }
        is RecordRef -> {
            if (RecordRef.isNotEmpty(value)) add(value)
        }
        is CamundaField -> {
            if (value.stringValue?.value?.isNotBlank() == true) add(value)
        }
        is CamundaFailedJobRetryTimeCycle -> {
            if (value.value?.isNotBlank() == true) add(value)
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

fun conditionFromAttributes(atts: Map<QName, String>): BpmnConditionDef {
    return BpmnConditionDef(
        type = atts[BPMN_PROP_CONDITION_TYPE]?.let {
            ConditionType.valueOf(it.uppercase())
        } ?: ConditionType.NONE,
        config = atts[BPMN_PROP_CONDITION_CONFIG]?.let {
            Json.mapper.read(it)?.let { node ->
                ConditionConfig(
                    fn = node["fn"].asText(),
                    expression = node["expression"].asText(),
                    outcome = Outcome(node["outcome"].asText())
                )
            } ?: ConditionConfig()
        } ?: ConditionConfig()
    )
}

fun Outcome.toTExpression(): TExpression {
    val exp = TExpression()
    exp.content.add(this.toExpressionStr())
    exp.otherAttributes[XSI_TYPE] = BPMN_T_FORMAT_EXPRESSION
    return exp
}

fun Outcome.toExpressionStr(): String {
    if (this == Outcome.EMPTY) {
        return ""
    }
    return "\${${fullId()} == '$value'}"
}

fun Outcome.fullId(): String {
    return id + "_" + OUTCOME_VAR
}

fun ConditionConfig.expressionToTExpression(): TExpression {
    val exp = TExpression()
    exp.content.add(expression)
    exp.otherAttributes[XSI_TYPE] = BPMN_T_FORMAT_EXPRESSION
    return exp
}

fun ConditionConfig.scriptToTExpression(): TExpression {
    val exp = TExpression()
    exp.content.add(fn)
    exp.otherAttributes[XSI_TYPE] = BPMN_T_FORMAT_EXPRESSION
    exp.otherAttributes[SCRIPT_LANGUAGE_ATTRIBUTE] = DEFAULT_SCRIPT_ENGINE_LANGUAGE
    return exp
}

fun BpmnScriptTaskDef.scriptPayloadToTScript(): TScript {
    return TScript().apply {
        content.add(script)
    }
}
