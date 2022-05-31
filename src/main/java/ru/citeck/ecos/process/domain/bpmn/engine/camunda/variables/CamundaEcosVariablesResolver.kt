package ru.citeck.ecos.process.domain.bpmn.engine.camunda.variables

import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT_REF
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService

/**
 * @author Roman Makarskiy
 */
class CamundaEcosVariablesResolver(
    private val variableScope: VariableScope, private val recordsScriptService: RecordsScriptService
) : Resolver {

    companion object {
        private val KEY_SET = mutableSetOf(VAR_DOCUMENT)
    }

    override fun containsKey(key: Any?): Boolean {
        return when (key) {
            VAR_DOCUMENT -> {
                variableScope.hasVariable(VAR_DOCUMENT_REF)
            }
            else -> false
        }

    }

    override fun get(key: Any?): Any? {
        return when (key) {
            VAR_DOCUMENT -> {
                recordsScriptService.get(variableScope.getVariable(VAR_DOCUMENT_REF))
            }
            else -> null
        }
    }

    override fun keySet(): MutableSet<String> {
        return KEY_SET
    }

}
