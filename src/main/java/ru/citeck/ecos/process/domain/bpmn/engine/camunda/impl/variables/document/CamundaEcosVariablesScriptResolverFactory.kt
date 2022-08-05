package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.document

import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory

/**
 * @author Roman Makarskiy
 */
// TODO: remove?
class CamundaEcosVariablesScriptResolverFactory : ResolverFactory {

    override fun createResolver(variableScope: VariableScope?): Resolver? {
        if (variableScope == null) return null
        return CamundaEcosVariablesScriptResolver(variableScope)
    }
}
