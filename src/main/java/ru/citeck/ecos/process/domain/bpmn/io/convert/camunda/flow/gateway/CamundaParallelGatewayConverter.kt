package ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.flow.gateway

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.i18n.I18nContext
import ru.citeck.ecos.process.domain.bpmn.io.convert.addIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_AFTER
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_ASYNC_BEFORE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_EXCLUSIVE
import ru.citeck.ecos.process.domain.bpmn.io.convert.camunda.CAMUNDA_JOB_PRIORITY
import ru.citeck.ecos.process.domain.bpmn.io.convert.jaxb
import ru.citeck.ecos.process.domain.bpmn.io.convert.putIfNotBlank
import ru.citeck.ecos.process.domain.bpmn.model.camunda.CamundaFailedJobRetryTimeCycle
import ru.citeck.ecos.process.domain.bpmn.model.ecos.flow.gateway.BpmnParallelGatewayDef
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExtensionElements
import ru.citeck.ecos.process.domain.bpmn.model.omg.TParallelGateway
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.EcosOmgConverter
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ImportContext
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName

class CamundaParallelGatewayConverter : EcosOmgConverter<BpmnParallelGatewayDef, TParallelGateway> {

    override fun import(element: TParallelGateway, context: ImportContext): BpmnParallelGatewayDef {
        error("Not supported")
    }

    override fun export(element: BpmnParallelGatewayDef, context: ExportContext): TParallelGateway {
        return TParallelGateway().apply {
            id = element.id
            name = MLText.getClosestValue(element.name, I18nContext.getLocale())

            element.incoming.forEach { incoming.add(QName("", it)) }
            element.outgoing.forEach { outgoing.add(QName("", it)) }

            otherAttributes[CAMUNDA_ASYNC_BEFORE] = element.asyncConfig.asyncBefore.toString()
            otherAttributes[CAMUNDA_ASYNC_AFTER] = element.asyncConfig.asyncAfter.toString()
            otherAttributes[CAMUNDA_EXCLUSIVE] = element.asyncConfig.exclusive.toString()

            otherAttributes.putIfNotBlank(CAMUNDA_JOB_PRIORITY, element.jobConfig.jobPriority.toString())

            extensionElements = TExtensionElements().apply {
                any.addAll(getJobRetryTimeCycleFieldConfig(element, context))
            }
        }
    }

    private fun getJobRetryTimeCycleFieldConfig(
        element: BpmnParallelGatewayDef,
        context: ExportContext
    ): List<JAXBElement<CamundaFailedJobRetryTimeCycle>> {
        val fields = mutableListOf<CamundaFailedJobRetryTimeCycle>()

        fields.addIfNotBlank(
            CamundaFailedJobRetryTimeCycle().apply {
                value = element.jobConfig.jobRetryTimeCycle
            }
        )

        return fields.map { it.jaxb(context) }
    }
}
