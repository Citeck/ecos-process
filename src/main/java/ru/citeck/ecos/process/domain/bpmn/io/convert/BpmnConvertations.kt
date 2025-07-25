package ru.citeck.ecos.process.domain.bpmn.io.convert

import jakarta.xml.bind.JAXBElement
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_SCRIPT_ENGINE_LANGUAGE
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.DEFAULT_IN_VARIABLES_PROPAGATION_TO_CALL_ACTIVITY
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.camunda.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnArtifactDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.artifact.BpmnTextAnnotationDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.MultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.VariablesMappingPropagation
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnColoredDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.BoundsDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.math.PointDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.BpmnConditionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.ConditionType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.expression.Outcome
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.BpmnFlowElementDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnAbstractEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnConditionalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.BpmnTerminateEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.error.BpmnErrorEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.BpmnSignalEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.signal.StatusChangeType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.event.timer.BpmnTimerEventDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.Recipient
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.RecipientType
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnAbstractEcosTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnAiTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.BpmnSetStatusTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_AI
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.ecos.ECOS_TASK_SET_STATUS
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.script.BpmnScriptTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.*
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.webapp.api.entity.EntityRef
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

    fun expression(name: String, expression: String): CamundaField {
        val expressionValue = CamundaExpression().apply {
            this.value = expression
        }

        return CamundaField().apply {
            this.name = name
            this.expressionValue = expressionValue
        }
    }
}

object CamundaPropertyCreator {

    fun string(name: String, value: String): CamundaProperty {
        return CamundaProperty().apply {
            this.name = name
            this.value = value
        }
    }
}

fun CamundaField.jaxb(context: ExportContext): JAXBElement<CamundaField> {
    return context.converters.convertToJaxb(this)
}

fun CamundaProperties.jaxb(context: ExportContext): JAXBElement<CamundaProperties> {
    return context.converters.convertToJaxb(this)
}

fun CamundaProperty.jaxb(context: ExportContext): JAXBElement<CamundaProperty> {
    return context.converters.convertToJaxb(this)
}

fun CamundaFailedJobRetryTimeCycle.jaxb(context: ExportContext): JAXBElement<CamundaFailedJobRetryTimeCycle> {
    return context.converters.convertToJaxb(this)
}

fun CamundaIn.jaxb(context: ExportContext): JAXBElement<CamundaIn> {
    return context.converters.convertToJaxb(this)
}

fun CamundaOut.jaxb(context: ExportContext): JAXBElement<CamundaOut> {
    return context.converters.convertToJaxb(this)
}

inline fun <K, reified V> MutableMap<in K, in V>.putIfNotBlank(key: K, value: V?) {
    if (value == null) return

    when (value) {
        is String -> {
            if (value.isNotBlank() && value != "null") put(key, value)
        }

        is EntityRef -> {
            if (EntityRef.isNotEmpty(value)) put(key, value)
        }

        else -> error("Type ${V::class} is not supported. Value: $value")
    }
}

inline fun <reified T> MutableList<in T>.addIfNotBlank(value: T?) {
    if (value == null) return

    when (value) {
        is String -> {
            if (value.isNotBlank() && value != "null") add(value)
        }

        is EntityRef -> {
            if (EntityRef.isNotEmpty(value)) add(value)
        }

        is CamundaField -> {
            if (value.stringValue?.value?.isNotBlank() == true) add(value)
            if (value.expressionValue?.value?.isNotBlank() == true) add(value)
        }

        is CamundaProperty -> {
            if (value.value?.isNotBlank() == true && value.name?.isNotBlank() == true) add(value)
        }

        is CamundaFailedJobRetryTimeCycle -> {
            if (value.value?.isNotBlank() == true) add(value)
        }

        else -> error("Type ${T::class} is not supported. $value")
    }
}

fun recipientsFromJson(recipientData: Map<RecipientType, String?>): List<Recipient> {
    if (recipientData.isEmpty()) {
        return emptyList()
    }

    val result = mutableListOf<Recipient>()

    for ((type, data) in recipientData) {
        Json.mapper.readList(data, String::class.java)
            .filter { it.isNotBlank() }
            .forEach { result.add(Recipient(type, it)) }
    }

    return result
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
            Json.mapper.read(it, ConditionConfig::class.java)
        } ?: ConditionConfig()
    )
}

fun bpmnColoredFromAttributes(atts: Map<QName, String>): BpmnColoredDef? {
    val backgroundColor = atts[BPMN_COLOR_BACKGROUND_COLOR]
    val borderColor = atts[BPMN_COLOR_BORDER_COLOR]
    val stroke = atts[BPMN_BIOCOLOR_STROKE]
    val fill = atts[BPMN_BIOCOLOR_FILL]

    if (backgroundColor != null && borderColor != null && stroke != null && fill != null) {
        return BpmnColoredDef(
            backgroundColor = backgroundColor,
            borderColor = borderColor,
            stroke = stroke,
            fill = fill
        )
    }

    return null
}

fun getCamundaBusinessKeyPropagationInInElement(
    businessKey: String?,
    context: ExportContext
): JAXBElement<CamundaIn> {
    val camundaIn = CamundaIn()
    camundaIn.businessKey = businessKey

    return camundaIn.jaxb(context)
}

fun getDefaultInVariablesPropagationToCallActivity(
    context: ExportContext
): List<JAXBElement<CamundaIn>> {
    return DEFAULT_IN_VARIABLES_PROPAGATION_TO_CALL_ACTIVITY.map { variable ->
        CamundaIn().apply {
            source = variable
            target = variable
        }.jaxb(context)
    }
}

fun getCamundaJobRetryTimeCycleFieldConfig(
    timeCycleValue: String?,
    context: ExportContext
): List<JAXBElement<CamundaFailedJobRetryTimeCycle>> {
    val fields = mutableListOf<CamundaFailedJobRetryTimeCycle>()

    fields.addIfNotBlank(
        CamundaFailedJobRetryTimeCycle().apply {
            value = timeCycleValue
        }
    )

    return fields.map { it.jaxb(context) }
}

internal fun String.isExpression(): Boolean {
    return startsWith("#{") || startsWith("\${")
}

fun convertManualStatusSelectionToPredicate(statusChangeType: StatusChangeType?, status: String?): Predicate? {
    if (status.isNullOrBlank() || statusChangeType == null) {
        return null
    }

    return Predicates.eq(statusChangeType.name.lowercase(), status)
}

fun VariablesMappingPropagation.toCamundaInElements(context: ExportContext): List<JAXBElement<CamundaIn>> {
    val variablesMapping = this
    val camundaElements = mutableListOf<CamundaIn>()

    if (propagateAllVariable) {
        camundaElements.add(
            CamundaIn().apply {
                variables = "all"
                local = variablesMapping.local.toString()
            }
        )
    }

    variablesMapping.variables.forEach { variable ->
        camundaElements.add(
            CamundaIn().apply {
                if (variable.source.isExpression()) {
                    sourceExpression = variable.source
                } else {
                    source = variable.source
                }

                target = variable.target
                local = variable.local.toString()
            }
        )
    }

    return camundaElements.map { it.jaxb(context) }
}

fun VariablesMappingPropagation.toCamundaOutElements(context: ExportContext): List<JAXBElement<CamundaOut>> {
    val variablesMapping = this

    val camundaElements = mutableListOf<CamundaOut>()

    if (propagateAllVariable) {
        camundaElements.add(
            CamundaOut().apply {
                variables = "all"
                local = variablesMapping.local.toString()
            }
        )
    }

    variablesMapping.variables.forEach { variable ->
        camundaElements.add(
            CamundaOut().apply {
                if (variable.source.isExpression()) {
                    sourceExpression = variable.source
                } else {
                    source = variable.source
                }

                target = variable.target
                local = variable.local.toString()
            }
        )
    }

    return camundaElements.map { it.jaxb(context) }
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
    return "\${${outcomeId()} == '$value'}"
}

fun ConditionConfig.expressionToTExpression(): TExpression {
    return createTExpressionWithContent(expression)
}

fun createTExpressionWithContent(content: String): TExpression {
    val exp = TExpression()
    exp.content.add(content)
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

fun BpmnTextAnnotationDef.toTText(): TText {
    return TText().apply {
        content.add(MLText.getClosestValue(text, I18nContext.getLocale()))
    }
}

fun TActivity.toMultiInstanceConfig(): MultiInstanceConfig? {
    if (loopCharacteristics == null) {
        return null
    }

    when (val loopConfig = loopCharacteristics.value) {
        is TMultiInstanceLoopCharacteristics -> {
            val config = Json.mapper.convert(
                otherAttributes[BPMN_MULTI_INSTANCE_CONFIG],
                MultiInstanceConfig::class.java
            ) ?: MultiInstanceConfig()
            config.sequential = loopConfig.isIsSequential

            return config
        }

        TStandardLoopCharacteristics::class.java -> {
            error("Loop is not supported by engine")
        }

        else -> error("Unsupported type of loop characteristics. Activity id '$id'")
    }
}

fun MultiInstanceConfig.toTLoopCharacteristics(context: ExportContext): TLoopCharacteristics {
    val config = this

    return TMultiInstanceLoopCharacteristics().apply {
        isIsSequential = sequential

        if (!config.loopCardinality.isNullOrBlank()) {
            loopCardinality = TExpression().apply {
                content.add(config.loopCardinality)
                otherAttributes[XSI_TYPE] = BPMN_T_FORMAT_EXPRESSION
            }
        }

        if (!config.completionCondition.isNullOrBlank()) {
            completionCondition = TExpression().apply {
                content.add(config.completionCondition)
                otherAttributes[XSI_TYPE] = BPMN_T_FORMAT_EXPRESSION
            }
        }

        otherAttributes.putIfNotBlank(CAMUNDA_COLLECTION, config.collection)
        otherAttributes.putIfNotBlank(CAMUNDA_ELEMENT_VARIABLE, config.element)

        otherAttributes[CAMUNDA_ASYNC_BEFORE] = config.asyncConfig.asyncBefore.toString()
        otherAttributes[CAMUNDA_ASYNC_AFTER] = config.asyncConfig.asyncAfter.toString()
        otherAttributes[CAMUNDA_EXCLUSIVE] = config.asyncConfig.exclusive.toString()

        extensionElements = TExtensionElements().apply {
            any.addAll(getCamundaJobRetryTimeCycleFieldConfig(config.jobConfig.jobRetryTimeCycle, context))
        }
    }
}

fun TTask.convertToBpmnEcosTaskDef(): BpmnAbstractEcosTaskDef? {
    val taskType = otherAttributes[BPMN_PROP_ECOS_TASK_TYPE] ?: return null
    if (taskType.isBlank()) {
        return null
    }

    return when (taskType) {
        ECOS_TASK_SET_STATUS -> {
            val status = otherAttributes[BPMN_PROP_ECOS_STATUS] ?: error("Status is not set")
            BpmnSetStatusTaskDef(status)
        }

        ECOS_TASK_AI -> {
            BpmnAiTaskDef(
                userInput = otherAttributes[BPMN_PROP_AI_USER_INPUT] ?: "",
                preProcessedScript = otherAttributes[BPMN_PROP_AI_PREPROCESSING_SCRIPT] ?: "",
                postProcessedScript = otherAttributes[BPMN_PROP_AI_POSTPROCESSING_SCRIPT] ?: "",
                addDocumentToContext = otherAttributes[BPMN_PROP_AI_ADD_DOCUMENT_TO_CONTEXT]?.toBoolean() ?: true,
                saveResultToDocumentAtt = otherAttributes[BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT] ?: ""
            )
        }

        else -> error("Unsupported task type: $taskType")
    }
}

fun TTask.fillEcosTaskDefToOtherAttributes(ecosTaskDef: BpmnAbstractEcosTaskDef) {
    when (ecosTaskDef) {
        is BpmnSetStatusTaskDef -> {
            otherAttributes[BPMN_PROP_ECOS_TASK_TYPE] = ECOS_TASK_SET_STATUS
            otherAttributes[BPMN_PROP_ECOS_STATUS] = ecosTaskDef.status
        }

        is BpmnAiTaskDef -> {
            otherAttributes[BPMN_PROP_ECOS_TASK_TYPE] = ECOS_TASK_AI
            otherAttributes[BPMN_PROP_AI_USER_INPUT] = ecosTaskDef.userInput
            otherAttributes[BPMN_PROP_AI_PREPROCESSING_SCRIPT] = ecosTaskDef.preProcessedScript
            otherAttributes[BPMN_PROP_AI_POSTPROCESSING_SCRIPT] = ecosTaskDef.postProcessedScript
            otherAttributes[BPMN_PROP_AI_ADD_DOCUMENT_TO_CONTEXT] = ecosTaskDef.addDocumentToContext.toString()
            otherAttributes[BPMN_PROP_AI_SAVE_RESULT_TO_DOCUMENT_ATT] = ecosTaskDef.saveResultToDocumentAtt
        }

        else -> error("Unsupported task type: $ecosTaskDef")
    }
}

fun TCatchEvent.convertToBpmnEventDef(context: ImportContext): BpmnAbstractEventDef? {
    return convertTEventDefinitionToBpmnEventDef(eventDefinition, this, context)
}

fun TThrowEvent.convertToBpmnEventDef(context: ImportContext): BpmnAbstractEventDef? {
    return convertTEventDefinitionToBpmnEventDef(eventDefinition, this, context)
}

private fun convertTEventDefinitionToBpmnEventDef(
    eventDefinition: List<JAXBElement<out TEventDefinition>>,
    event: TEvent,
    context: ImportContext
): BpmnAbstractEventDef? {
    if (eventDefinition.isEmpty()) {
        return null
    }

    if (eventDefinition.size != 1) {
        error("Not supported state. Check implementation.")
    }

    val eventDef = eventDefinition[0].value
    val typeToTransform = when (val type = eventDefinition[0].declaredType) {
        TTimerEventDefinition::class.java -> BpmnTimerEventDef::class.java
        TSignalEventDefinition::class.java -> BpmnSignalEventDef::class.java
        TErrorEventDefinition::class.java -> BpmnErrorEventDef::class.java
        TTerminateEventDefinition::class.java -> BpmnTerminateEventDef::class.java
        TConditionalEventDefinition::class.java -> BpmnConditionalEventDef::class.java
        else -> error("Class $type not supported")
    }

    provideOtherAttsToEventDef(eventDef, event)

    val convertedEvent = context.converters.import(
        eventDef,
        typeToTransform,
        context
    ).data

    convertedEvent.elementId = event.id

    return convertedEvent
}

fun TCatchEvent.fillBpmnEventDefPayloadFromBpmnEventDef(bpmnEventDef: BpmnAbstractEventDef, context: ExportContext) {
    fillBpmnEventDefPayloadFromBpmnEventDef(bpmnEventDef, this, this.eventDefinition, context)
}

fun TThrowEvent.fillBpmnEventDefPayloadFromBpmnEventDef(bpmnEventDef: BpmnAbstractEventDef, context: ExportContext) {
    fillBpmnEventDefPayloadFromBpmnEventDef(bpmnEventDef, this, this.eventDefinition, context)
}

private fun fillBpmnEventDefPayloadFromBpmnEventDef(
    bpmnEventDef: BpmnAbstractEventDef,
    event: TEvent,
    eventDefinition: MutableList<JAXBElement<out TEventDefinition>>,
    context: ExportContext
) {
    when (bpmnEventDef) {
        is BpmnTimerEventDef -> {
            event.otherAttributes.putIfNotBlank(BPMN_PROP_TIME_CONFIG, Json.mapper.toString(bpmnEventDef.value))
        }

        is BpmnSignalEventDef -> {
            if (bpmnEventDef.eventType == EcosEventType.USER_EVENT) {
                event.otherAttributes.putIfNotBlank(BPMN_PROP_USER_EVENT, bpmnEventDef.manualSignalName)
            } else {
                event.otherAttributes.putIfNotBlank(BPMN_PROP_MANUAL_SIGNAL_NAME, bpmnEventDef.manualSignalName)
            }

            val filterByPredicate =
                if (bpmnEventDef.eventType == EcosEventType.RECORD_STATUS_CHANGED &&
                    bpmnEventDef.statusChangeType != null &&
                    bpmnEventDef.manualStatus != null
                ) {
                    null
                } else {
                    bpmnEventDef.eventFilterByPredicate
                }

            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_EVENT_FILTER_BY_PREDICATE,
                Json.mapper.toString(filterByPredicate)
            )
            event.otherAttributes.putIfNotBlank(BPMN_PROP_STATUS_CHANGE_TYPE, bpmnEventDef.statusChangeType?.name)
            event.otherAttributes.putIfNotBlank(BPMN_PROP_MANUAL_STATUS, bpmnEventDef.manualStatus)
            event.otherAttributes[BPMN_PROP_EVENT_MANUAL_MODE] = bpmnEventDef.eventManualMode.toString()
            event.otherAttributes.putIfNotBlank(BPMN_PROP_EVENT_TYPE, bpmnEventDef.eventType?.name)
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_EVENT_FILTER_BY_RECORD_TYPE,
                bpmnEventDef.eventFilterByRecordType.name
            )
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_EVENT_FILTER_BY_ECOS_TYPE,
                bpmnEventDef.eventFilterByEcosType.toString()
            )
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_EVENT_FILTER_BY_RECORD_VARIABLE,
                bpmnEventDef.eventFilterByRecordVariable
            )
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_EVENT_MODEL,
                Json.mapper.toString(bpmnEventDef.eventModel)
            )
        }

        is BpmnErrorEventDef -> {
            event.otherAttributes[BPMN_PROP_ERROR_NAME] = bpmnEventDef.errorName

            event.otherAttributes.putIfNotBlank(BPMN_PROP_ERROR_CODE, bpmnEventDef.errorCode)
            event.otherAttributes.putIfNotBlank(BPMN_PROP_ERROR_MESSAGE, bpmnEventDef.errorMessage)

            event.otherAttributes.putIfNotBlank(BPMN_PROP_ERROR_CODE_VARIABLE, bpmnEventDef.errorCodeVariable)
            event.otherAttributes.putIfNotBlank(BPMN_PROP_ERROR_MESSAGE_VARIABLE, bpmnEventDef.errorMessageVariable)
        }

        is BpmnTerminateEventDef -> {
            // do nothing
        }

        is BpmnConditionalEventDef -> {
            event.otherAttributes[BPMN_PROP_REACT_ON_DOCUMENT_CHANGE] = bpmnEventDef.reactOnDocumentChange.toString()
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_DOCUMENT_VARIABLES,
                Json.mapper.toString(bpmnEventDef.documentVariables)
            )

            event.otherAttributes.putIfNotBlank(BPMN_PROP_VARIABLE_NAME, bpmnEventDef.variableName)
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_VARIABLE_EVENTS,
                Json.mapper.toString(bpmnEventDef.variableEvents)
            )

            event.otherAttributes.putIfNotBlank(BPMN_PROP_CONDITION_TYPE, bpmnEventDef.condition.type.name)
            event.otherAttributes.putIfNotBlank(
                BPMN_PROP_CONDITION_CONFIG,
                Json.mapper.toString(bpmnEventDef.condition.config)
            )
        }

        else -> error("Class $bpmnEventDef not supported")
    }

    val typeToTransform = when (val type = bpmnEventDef.javaClass) {
        BpmnTimerEventDef::class.java -> TTimerEventDefinition::class.java
        BpmnSignalEventDef::class.java -> TSignalEventDefinition::class.java
        BpmnErrorEventDef::class.java -> TErrorEventDefinition::class.java
        BpmnTerminateEventDef::class.java -> TTerminateEventDefinition::class.java
        BpmnConditionalEventDef::class.java -> TConditionalEventDefinition::class.java
        else -> error("Class $type not supported")
    }

    val eventDef = context.converters.export(bpmnEventDef, typeToTransform, context)
    eventDefinition.add(context.converters.convertToJaxb(eventDef))
}

fun TCatchEvent.fillCamundaEventDefPayloadFromBpmnEventDef(
    bpmnEventDef: BpmnAbstractEventDef,
    jobConfig: JobConfig,
    context: ExportContext
) {
    fillCamundaEventDefPayloadFromBpmnEventDef(bpmnEventDef, this, this.eventDefinition, jobConfig, context)
}

fun TThrowEvent.fillCamundaEventDefPayloadFromBpmnEventDef(
    bpmnEventDef: BpmnAbstractEventDef,
    jobConfig: JobConfig,
    context: ExportContext
) {
    fillCamundaEventDefPayloadFromBpmnEventDef(bpmnEventDef, this, this.eventDefinition, jobConfig, context)
}

private fun fillCamundaEventDefPayloadFromBpmnEventDef(
    bpmnEventDef: BpmnAbstractEventDef,
    event: TEvent,
    eventDefinition: MutableList<JAXBElement<out TEventDefinition>>,
    jobConfig: JobConfig,
    context: ExportContext
) {
    val typeToTransform = when (val type = bpmnEventDef.javaClass) {
        BpmnTimerEventDef::class.java -> TTimerEventDefinition::class.java
        BpmnSignalEventDef::class.java -> TSignalEventDefinition::class.java
        BpmnErrorEventDef::class.java -> TErrorEventDefinition::class.java
        BpmnTerminateEventDef::class.java -> TTerminateEventDefinition::class.java
        BpmnConditionalEventDef::class.java -> TConditionalEventDefinition::class.java
        else -> error("Class $type not supported")
    }

    val eventDef = context.converters.export(bpmnEventDef, typeToTransform, context)
    eventDefinition.add(context.converters.convertToJaxb(eventDef))

    event.extensionElements = TExtensionElements().apply {
        any.addAll(getCamundaJobRetryTimeCycleFieldConfig(jobConfig.jobRetryTimeCycle, context))
    }
}

fun provideOtherAttsToEventDef(eventDef: TEventDefinition, element: TEvent) {
    element.otherAttributes.forEach { (k, v) ->
        eventDef.otherAttributes[k] = v
    }
}

fun TFlowElement.toBpmnFlowElementDef(context: ImportContext): BpmnFlowElementDef {
    val flowElement = context.converters.import(this, context)

    return BpmnFlowElementDef(
        id = this.id,
        type = flowElement.type,
        data = flowElement.data
    )
}

fun TArtifact.toBpmnArtifactDef(context: ImportContext): BpmnArtifactDef {
    val artifact = context.converters.import(this, context)

    return BpmnArtifactDef(
        id = this.id,
        type = artifact.type,
        data = artifact.data
    )
}

fun QName.toCamundaKey(): String {
    return "$namespaceURI:$localPart"
}
