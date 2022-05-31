package ru.citeck.ecos.process.domain.bpmn.engine.camunda.variables

import org.camunda.bpm.engine.delegate.VariableScope
import org.camunda.bpm.engine.impl.scripting.engine.Resolver
import org.camunda.bpm.engine.impl.scripting.engine.ResolverFactory
import ru.citeck.ecos.records3.record.atts.computed.script.RecordsScriptService

/**
 * @author Roman Makarskiy
 */
class CamundaEcosVariablesResolverFactory(
    private val recordsScriptService: RecordsScriptService
) : ResolverFactory {

    override fun createResolver(variableScope: VariableScope?): Resolver? {
        if (variableScope == null) return null
        return CamundaEcosVariablesResolver(variableScope, recordsScriptService)
    }

}
