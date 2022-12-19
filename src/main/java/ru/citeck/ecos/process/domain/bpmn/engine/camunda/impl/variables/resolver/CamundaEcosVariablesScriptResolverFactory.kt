package ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.variables.resolver

import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory
import ru.citeck.ecos.webapp.api.properties.EcosWebAppProps

/**
 * @author Roman Makarskiy
 */
class CamundaEcosVariablesScriptResolverFactory(
    private val ecosWebAppProps: EcosWebAppProps
) : ResolverFactory {

    override fun createResolver(variableScope: VariableScope?): Resolver {
        return CamundaEcosVariablesScriptResolver(ecosWebAppProps)
    }
}
