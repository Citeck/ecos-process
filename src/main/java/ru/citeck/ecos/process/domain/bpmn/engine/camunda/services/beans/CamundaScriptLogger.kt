package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import mu.KotlinLogging
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.springframework.stereotype.Component

@Component
class CamundaScriptLogger : CamundaProcessEngineService {

    override fun getKey(): String {
        return "scriptLogger"
    }

    fun get(execution: DelegateExecution): ScriptLogger {
        return ScriptLogger(execution)
    }
}

class ScriptLogger(
    private val execution: DelegateExecution
) {

    companion object {
        val log = KotlinLogging.logger {}
    }

    fun info(message: String) {
        log.info { appendExecutionInfo(message) }
    }

    fun error(message: String) {
        log.error { appendExecutionInfo(message) }
    }

    fun warn(message: String) {
        log.warn { appendExecutionInfo(message) }
    }

    fun debug(message: String) {
        log.debug { appendExecutionInfo(message) }
    }

    fun trace(message: String) {
        log.trace { appendExecutionInfo(message) }
    }

    private fun appendExecutionInfo(message: String): String {
        return "\n|businessKey: ${execution.businessKey}" +
            "\n|activity: ${execution.currentActivityId} ${execution.currentActivityName}" +
            "\n|processDefId: ${execution.processDefinitionId}, processInstId: ${execution.processInstanceId}" +
            "\n|Message:" +
            "\n$message"
    }

}
