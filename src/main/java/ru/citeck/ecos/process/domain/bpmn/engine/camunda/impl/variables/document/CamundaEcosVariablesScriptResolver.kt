package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.document

import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.VAR_DOCUMENT

/**
 * @author Roman Makarskiy
 */
// TODO: fill predefined variables or remove?
class CamundaEcosVariablesScriptResolver(
    private val variableScope: VariableScope
) : Resolver {

    companion object {
        private val KEY_SET = mutableSetOf(VAR_DOCUMENT)
    }

    override fun containsKey(key: Any?): Boolean {
        return false

       /* return when (key) {
            VAR_DOCUMENT -> {
                variableScope.hasVariable(VAR_DOCUMENT_REF)
            }
            else -> false
        }*/
    }

    override fun get(key: Any?): Any? {
       /* return when (key) {
            VAR_DOCUMENT -> {
                recordsScriptService.get(variableScope.getVariable(VAR_DOCUMENT_REF))
            }
            else -> null
        }*/
        return null
    }

    override fun keySet(): MutableSet<String> {
        return mutableSetOf()
    }
}
