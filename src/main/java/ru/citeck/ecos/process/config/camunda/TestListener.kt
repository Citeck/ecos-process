package ru.citeck.ecos.process.config.camunda

import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.ExecutionListener
import org.springframework.stereotype.Component

//TODO: remove
@Component
class TestListener: ExecutionListener {
    override fun notify(execution: DelegateExecution) {

        val variables = execution.variables

        val hasVariablesLocal = execution.variablesLocal

        val variableNames = execution.variableNames

        println("=========")
        println("listener call")
        println("=========")

    }
}
