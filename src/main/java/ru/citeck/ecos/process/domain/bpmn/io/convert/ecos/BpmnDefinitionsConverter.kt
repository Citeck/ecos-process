package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.common.generateElementId
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.error.BpmnErrorDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.pool.BpmnCollaborationDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.signal.BpmnSignalDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.*
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val SIGNAL_PREFIX = "Signal"
private const val ERROR_PREFIX = "Error"

class BpmnDefinitionsConverter : EcosOmgConverter<BpmnDefinitionDef, TDefinitions> {

    override fun import(element: TDefinitions, context: ImportContext): BpmnDefinitionDef {

        val processDefId = element.otherAttributes[BPMN_PROP_PROCESS_DEF_ID]
        if (processDefId.isNullOrBlank()) {
            propMandatoryError(BPMN_PROP_PROCESS_DEF_ID, BpmnDefinitionDef::class)
        }

        val (processes, collaboration) = element.extractRootElements(context)

        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        val result = BpmnDefinitionDef(
            id = processDefId,
            enabled = element.otherAttributes[BPMN_PROP_ENABLED].toBoolean(),
            autoStartEnabled = element.otherAttributes[BPMN_PROP_AUTO_START_ENABLED].toBoolean(),
            definitionsId = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            ecosType = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_ECOS_TYPE]),
            formRef = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_FORM_REF]),
            sectionRef = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_SECTION_REF]),
            diagrams = element.bpmnDiagram.map {
                context.converters.import(it, BpmnDiagramDef::class.java, context).data
            },
            process = processes,
            collaboration = collaboration,
            signals = context.generateSignalsFromDefs(),
            signalsEventDefsMeta = context.bpmnSignalEventDefs,
            conditionalEventDefsMeta = context.conditionalEventDefs,
            errors = context.generateErrorsFromDefs(),
            errorsEventDefsMeta = context.bpmnErrorEventDefs.values.toList(),
            exporter = element.exporter,
            exporterVersion = element.exporterVersion,
            targetNamespace = element.targetNamespace
        )

        return result
    }

    override fun export(element: BpmnDefinitionDef, context: ExportContext): TDefinitions {
        element.signals.forEach {
            context.bpmnSignalsByNames.computeIfAbsent(it.name) { _ -> it }
        }

        element.errors.forEach {
            context.bpmnErrorsByNames.computeIfAbsent(it.name) { _ -> it }
        }

        return TDefinitions().apply {
            id = element.definitionsId
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())
            exporter = element.exporter
            exporterVersion = element.exporterVersion
            targetNamespace = element.targetNamespace

            element.process.forEach { process ->
                val tProcess = context.converters.export<TProcess>(process, context)
                rootElement.add(context.converters.convertToJaxb(tProcess))
            }

            element.collaboration?.let {
                val tCollaboration = context.converters.export<TCollaboration>(it, context)
                rootElement.add(context.converters.convertToJaxb(tCollaboration))
            }

            element.signals.forEach {
                val signal = context.converters.export<TSignal>(it, context)
                rootElement.add(context.converters.convertToJaxb(signal))
            }

            element.errors.forEach {
                val error = context.converters.export<TError>(it, context)
                rootElement.add(context.converters.convertToJaxb(error))
            }

            element.diagrams.forEach {
                bpmnDiagram.add(context.converters.export(it))
            }

            otherAttributes[BPMN_PROP_ENABLED] = element.enabled.toString()
            otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = element.autoStartEnabled.toString()
            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[BPMN_PROP_ECOS_TYPE] = element.ecosType.toString()
            otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = element.id
            otherAttributes[BPMN_PROP_FORM_REF] = element.formRef.toString()
            otherAttributes[BPMN_PROP_SECTION_REF] = element.sectionRef.toString()
            otherAttributes[BPMN_PROP_DEF_STATE] = ProcDefRevDataState.CONVERTED.name
        }
    }
}

private fun TDefinitions.extractRootElements(context: ImportContext): RootElements {
    val process = mutableListOf<BpmnProcessDef>()
    var collaboration: BpmnCollaborationDef? = null

    rootElement.forEach { rootElement ->
        when (rootElement.value) {
            is TProcess -> {
                val bpmnProcessDef =
                    context.converters.import(rootElement.value, BpmnProcessDef::class.java, context).data
                process.add(bpmnProcessDef)
            }

            is TSignal -> {
                // do nothing, we ourselves create signals from signal definitions
            }

            is TError -> {
                // do nothing, we ourselves create errors from error definitions
            }

            is TCollaboration -> {
                if (collaboration != null) {
                    throw IllegalStateException("Only one collaboration is supported")
                }

                collaboration = context.converters.import(
                    rootElement.value,
                    BpmnCollaborationDef::class.java,
                    context
                ).data
            }

            else -> error("Unsupported root element: ${rootElement.value}")
        }
    }

    return RootElements(process, collaboration)
}

private data class RootElements(
    val processes: List<BpmnProcessDef>,
    val collaboration: BpmnCollaborationDef? = null
)

private fun ImportContext.generateSignalsFromDefs(): List<BpmnSignalDef> {
    return this.bpmnSignalEventDefs.map {
        it.signalName
    }
        .distinct()
        .map {
            BpmnSignalDef(
                id = generateElementId(SIGNAL_PREFIX),
                name = it
            )
        }
}

private fun ImportContext.generateErrorsFromDefs(): List<BpmnErrorDef> {
    return this.bpmnErrorEventDefs
        .values
        .map {
            BpmnErrorDef(
                id = generateElementId(ERROR_PREFIX),
                name = it.errorName,
                errorCode = it.errorCode,
                errorMessage = it.errorMessage
            )
        }
}
