package ru.citeck.ecos.process.domain.bpmn.io.convert.ecos.process

import ru.citeck.ecos.process.domain.bpmn.io.propMandatoryError
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExclusiveGateway
import ru.citeck.ecos.process.domain.bpmn.model.omg.TSequenceFlow
import ru.citeck.ecos.process.domain.procdef.convert.io.convert.context.ExportContext

fun fillElementsRefsFromIdToRealObjects(tFlowElements: List<TBaseElement>, context: ExportContext) {
    tFlowElements.forEach { element ->
        if (element is TSequenceFlow) {
            element.sourceRef = context.bpmnElementsById[element.sourceRef.toString()]
                ?: propMandatoryError("sourceRef", element::class)
            element.targetRef = context.bpmnElementsById[element.targetRef.toString()]
                ?: propMandatoryError("targetRef", element::class)
        }

        if (element is TExclusiveGateway) {
            if (element.default != null) {
                element.default = context.bpmnElementsById[element.default]
                    ?: propMandatoryError("default", element::class)
            }
        }
    }
}
