package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnDefinitionDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.BpmnProcessDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.diagram.BpmnDiagramDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TDefinitions
import ru.citeck.ecos.process.domain.bpmn.model.omg.TProcess
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef

class BpmnDefinitionsConverter : EcosOmgConverter<BpmnDefinitionDef, TDefinitions> {

    override fun import(element: TDefinitions, context: ImportContext): BpmnDefinitionDef {

        if (element.rootElement.size > 1) error("Root elements is more than one not supported.")

        val process = element.rootElement[0].value as TProcess

        val processDefId = element.otherAttributes[BPMN_PROP_PROCESS_DEF_ID]
        if (processDefId.isNullOrBlank()) propMandatoryError(BPMN_PROP_PROCESS_DEF_ID, BpmnDefinitionDef::class)

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
            process = context.converters.import(process, BpmnProcessDef::class.java, context).data,
            exporter = element.exporter,
            exporterVersion = element.exporterVersion,
            targetNamespace = element.targetNamespace
        )

        return result
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

            otherAttributes[BPMN_PROP_ENABLED] = element.enabled.toString()
            otherAttributes[BPMN_PROP_AUTO_START_ENABLED] = element.autoStartEnabled.toString()
            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)
            otherAttributes[BPMN_PROP_ECOS_TYPE] = element.ecosType.toString()
            otherAttributes[BPMN_PROP_PROCESS_DEF_ID] = element.id
            otherAttributes[BPMN_PROP_FORM_REF] = element.formRef.toString()
        }
    }
}
