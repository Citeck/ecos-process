package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.flow.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toMultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.io.convert.toTLoopCharacteristics
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.RefBinding
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.VariablesMappingPropagation
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.callactivity.BpmnCallActivityDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCallActivity
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnCallActivityTaskConverter : EcosOmgConverter<BpmnCallActivityDef, TCallActivity> {
    override fun import(element: TCallActivity, context: ImportContext): BpmnCallActivityDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnCallActivityDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            number = element.otherAttributes[BPMN_PROP_NUMBER]?.takeIf { it.isNotEmpty() }?.toInt(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            processRef = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_PROCESS_REF]),
            calledElement = element.otherAttributes[BPMN_PROP_CALLED_ELEMENT],
            binding = RefBinding.valueOf(
                element.otherAttributes[BPMN_PROP_PROCESS_BINDING]
                    ?: error("Process Binding is required")
            ),
            version = let {
                val processVersionStr = element.otherAttributes[BPMN_PROP_PROCESS_VERSION]
                if (processVersionStr.isNullOrBlank()) {
                    null
                } else {
                    processVersionStr.toInt()
                }
            },
            versionTag = element.otherAttributes[BPMN_PROP_PROCESS_VERSION_TAG],
            inVariablePropagation = convertVariableMappingPropagationWithNotEmptySourceVariables(
                element.otherAttributes[BPMN_PROP_PROCESS_IN_VARIABLE_PROPAGATION]
            ),
            outVariablePropagation = convertVariableMappingPropagationWithNotEmptySourceVariables(
                element.otherAttributes[BPMN_PROP_PROCESS_OUT_VARIABLE_PROPAGATION]
            ),
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            multiInstanceConfig = element.toMultiInstanceConfig()
        )
    }

    private fun convertVariableMappingPropagationWithNotEmptySourceVariables(
        json: String?
    ): VariablesMappingPropagation {
        val converted = Json.mapper.read(json, VariablesMappingPropagation::class.java) ?: VariablesMappingPropagation()
        val cleanedVariables = converted.variables.filter { it.source.isNotBlank() }
        return converted.copy(variables = cleanedVariables)
    }

    override fun export(element: BpmnCallActivityDef, context: ExportContext): TCallActivity {
        return TCallActivity().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_DOC, Json.mapper.toString(element.documentation))
            otherAttributes[BPMN_PROP_PROCESS_REF] = element.processRef.toString()
            otherAttributes.putIfNotBlank(BPMN_PROP_CALLED_ELEMENT, element.calledElement)
            otherAttributes[BPMN_PROP_PROCESS_BINDING] = element.binding.name
            otherAttributes.putIfNotBlank(BPMN_PROP_PROCESS_VERSION, element.version?.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_PROCESS_VERSION_TAG, element.versionTag)

            otherAttributes.putIfNotBlank(
                BPMN_PROP_PROCESS_IN_VARIABLE_PROPAGATION,
                Json.mapper.toString(element.inVariablePropagation)
            )
            otherAttributes.putIfNotBlank(
                BPMN_PROP_PROCESS_OUT_VARIABLE_PROPAGATION,
                Json.mapper.toString(element.outVariablePropagation)
            )

            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))

            element.number?.let { otherAttributes.putIfNotBlank(BPMN_PROP_NUMBER, it.toString()) }
            element.multiInstanceConfig?.let {
                loopCharacteristics = context.converters.convertToJaxb(it.toTLoopCharacteristics(context))
                otherAttributes[BPMN_MULTI_INSTANCE_CONFIG] = Json.mapper.toString(it)
            }
        }
    }
}
