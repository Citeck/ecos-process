package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.process.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.signal.BpmnSignalDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSignal
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef

class BpmnDefinitionsConverter : EcosOmgConverter<BpmnDefinitionDef, TDefinitions> {

    override fun import(element: TDefinitions, context: ImportContext): BpmnDefinitionDef {

        val processDefId = element.otherAttributes[BPMN_PROP_PROCESS_DEF_ID]
        if (processDefId.isNullOrBlank()) propMandatoryError(BPMN_PROP_PROCESS_DEF_ID, BpmnDefinitionDef::class)

        val processes = element.extractRootElements(context).processes

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
            signals = context.generateSignalsFromDefs(),
            /*messages = emptyList(),*/
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

            element.signals.forEach {
                val signal = context.converters.export<TSignal>(it, context)
                rootElement.add(context.converters.convertToJaxb(signal))
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
        }
    }
}


private fun TDefinitions.extractRootElements(context: ImportContext): RootElements {
    val process = mutableListOf<BpmnProcessDef>()

    rootElement.forEach { rootElement ->
        when (rootElement.value) {
            is TProcess -> {
                val bpmnProcessDef =
                    context.converters.import(rootElement.value, BpmnProcessDef::class.java, context).data
                process.add(bpmnProcessDef)
            }

            is TSignal -> {
                //do nothing, we ourselves create signals from signal definitions
            }

            else -> error("Unsupported root element: ${rootElement.value}")
        }
    }

    return RootElements(process)
}


private data class RootElements(
    val processes: List<BpmnProcessDef>
)

private fun ImportContext.generateSignalsFromDefs(): List<BpmnSignalDef> {
    return this.bpmnSignalNames.map { signalName ->
        BpmnSignalDef(
            id = generateElementId("Signal"),
            name = signalName
        )
    }
        .toList()
}

private fun generateElementId(prefix: String): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return "${prefix}_" + (1..7)
        .map { allowedChars.random() }
        .joinToString("")
}
