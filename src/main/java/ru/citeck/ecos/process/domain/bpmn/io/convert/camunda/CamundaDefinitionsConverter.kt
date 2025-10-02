package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_PROCESS_DEF_ID
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_WORKSPACE
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TCollaboration
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TError
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSignal
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaDefinitionsConverter : EcosOmgConverter<BpmnDefinitionDef, TDefinitions> {

    override fun import(element: TDefinitions, context: ImportContext): BpmnDefinitionDef {
        error("Not supported")
    }

    override fun export(element: BpmnDefinitionDef, context: ExportContext): TDefinitions {
        element.signals.forEach {
            context.bpmnSignalsByNames.computeIfAbsent(it.name) { _ -> it }
        }

        element.errors.forEach {
            context.bpmnErrorsByNames.computeIfAbsent(it.name) { _ -> it }
        }

        context.setWorkspace(element.workspace)

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

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[BPMN_PROP_ECOS_TYPE] = element.ecosType.toString()
            otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = element.id
            otherAttributes[BPMN_PROP_WORKSPACE] = element.workspace
        }
    }
}
