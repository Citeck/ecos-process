package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_ECOS_TYPE
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_NAME_ML
import ru.citeck.ecos.process.domain.bpmn.io.BPMN_PROP_PROCESS_DEF_ID
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext

class CamundaDefinitionsConverter : EcosOmgConverter<BpmnDefinitionDef, TDefinitions> {

    override fun import(element: TDefinitions, context: ImportContext): BpmnDefinitionDef {
        error("Not supported")
    }

    override fun export(element: BpmnDefinitionDef, context: ExportContext): TDefinitions {
        return TDefinitions().apply {
            id = element.definitionsId
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())
            exporter = element.exporter
            exporterVersion = element.exporterVersion
            targetNamespace = element.targetNamespace

            // TODO: process single element?
            val process = context.converters.export<TProcess>(element.process)
            rootElement.add(context.converters.convertToJaxb(process))

            element.diagrams.forEach {
                bpmnDiagram.add(context.converters.export(it))
            }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[BPMN_PROP_ECOS_TYPE] = element.ecosType.toString()
            otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = element.id
        }
    }
}
