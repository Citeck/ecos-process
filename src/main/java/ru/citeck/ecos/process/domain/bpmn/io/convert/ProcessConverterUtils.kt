package ru.citeck.ecos.process.domain.bpmn.io.convert

import ru.citeck.ecos.process.domain.bpmn.io.propMandatoryError
import ru.citeck.ecos.process.domain.bpmn.model.omg.TActivity
import ru.citeck.ecos.process.domain.bpmn.model.omg.TBaseElement
import ru.citeck.ecos.process.domain.bpmn.model.omg.TComplexGateway
import ru.citeck.ecos.process.domain.bpmn.model.omg.TExclusiveGateway
import ru.citeck.ecos.process.domain.bpmn.model.omg.TInclusiveGateway
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

        // `default` is an IDREF that must be replaced by the referenced object before marshalling. The four
        // generated classes below each declare their own `default` property with no shared supertype, so each
        // needs its own branch; today converters populate it for exclusive/inclusive gateways only, but resolving
        // every declared carrier here keeps the marshalling safe if more of them become reachable.
        when (element) {
            is TExclusiveGateway -> element.default = resolveDefaultRef(element.default, element, context)
            is TInclusiveGateway -> element.default = resolveDefaultRef(element.default, element, context)
            is TComplexGateway -> element.default = resolveDefaultRef(element.default, element, context)
            is TActivity -> element.default = resolveDefaultRef(element.default, element, context)
        }
    }
}

private fun resolveDefaultRef(default: Any?, element: TBaseElement, context: ExportContext): TSequenceFlow? {
    if (default == null) {
        return null
    }
    val resolved = context.bpmnElementsById[default.toString()]
        ?: error("Element '${element.id}': default flow reference '$default' can't be resolved")
    return resolved as? TSequenceFlow
        ?: error("Element '${element.id}': default flow reference '$default' is not a sequence flow")
}
