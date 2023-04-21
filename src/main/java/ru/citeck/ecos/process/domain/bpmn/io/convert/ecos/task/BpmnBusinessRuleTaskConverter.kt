package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.task

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.*
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.toMultiInstanceConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.AsyncConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.common.async.JobConfig
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule.BpmnBusinessRuleTaskDef
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule.DecisionRefBinding
import ru.citeck.ecos.process.domain.bpmn.model.ecos.task.businessrule.MapDecisionResult
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBusinessRuleTask
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.xml.namespace.QName

class BpmnBusinessRuleTaskConverter : EcosOmgConverter<BpmnBusinessRuleTaskDef, TBusinessRuleTask> {

    override fun import(element: TBusinessRuleTask, context: ImportContext): BpmnBusinessRuleTaskDef {
        val name = element.otherAttributes[BPMN_PROP_NAME_ML] ?: element.name

        return BpmnBusinessRuleTaskDef(
            id = element.id,
            name = Json.mapper.convert(name, MLText::class.java) ?: MLText(),
            documentation = Json.mapper.convert(element.otherAttributes[BPMN_PROP_DOC], MLText::class.java) ?: MLText(),
            incoming = element.incoming.map { it.localPart },
            outgoing = element.outgoing.map { it.localPart },
            decisionRef = EntityRef.valueOf(element.otherAttributes[BPMN_PROP_DMN_DECISION_REF]),
            binding = DecisionRefBinding.valueOf(
                element.otherAttributes[BPMN_PROP_DMN_DECISION_BINDING]
                    ?: error("Decision Binding is required")
            ),
            version = let {
                val decisionVersionStr = element.otherAttributes[BPMN_PROP_DMN_DECISION_VERSION]
                if (decisionVersionStr.isNullOrBlank()) {
                    null
                } else {
                    decisionVersionStr.toInt()
                }
            },
            versionTag = element.otherAttributes[BPMN_PROP_DMN_DECISION_VERSION_TAG],
            resultVariable = element.otherAttributes[BPMN_PROP_RESULT_VARIABLE],
            mapDecisionResult = element.otherAttributes[BPMN_PROP_DMN_MAP_DECISION_RESULT]?.let {
                MapDecisionResult.valueOf(it)
            },
            asyncConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_ASYNC_CONFIG], AsyncConfig::class.java)
                ?: AsyncConfig(),
            jobConfig = Json.mapper.read(element.otherAttributes[BPMN_PROP_JOB_CONFIG], JobConfig::class.java)
                ?: JobConfig(),
            multiInstanceConfig = element.toMultiInstanceConfig()
        )
    }

    override fun export(element: BpmnBusinessRuleTaskDef, context: ExportContext): TBusinessRuleTask {
        return TBusinessRuleTask().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[BPMN_PROP_NAME_ML] = Json.mapper.toString(element.name)

            otherAttributes[BPMN_PROP_DMN_DECISION_REF] = element.decisionRef.toString()
            otherAttributes[BPMN_PROP_DMN_DECISION_BINDING] = element.binding.name
            otherAttributes.putIfNotBlank(BPMN_PROP_DMN_DECISION_VERSION, element.version?.toString())
            otherAttributes.putIfNotBlank(BPMN_PROP_DMN_DECISION_VERSION_TAG, element.versionTag)
            otherAttributes.putIfNotBlank(BPMN_PROP_RESULT_VARIABLE, element.resultVariable)
            otherAttributes.putIfNotBlank(BPMN_PROP_DMN_MAP_DECISION_RESULT, element.mapDecisionResult?.name)

            otherAttributes.putIfNotBlank(BPMN_PROP_ASYNC_CONFIG, Json.mapper.toString(element.asyncConfig))
            otherAttributes.putIfNotBlank(BPMN_PROP_JOB_CONFIG, Json.mapper.toString(element.jobConfig))
        }
    }
}
