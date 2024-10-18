package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import io.github.oshai.kotlinlogging.KotlinLogging
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

    fun info(message: Any) {
        log.info { appendExecutionInfo(message.toString()) }
    }

    fun error(message: Any) {
        log.error { appendExecutionInfo(message.toString()) }
    }

    fun warn(message: Any) {
        log.warn { appendExecutionInfo(message.toString()) }
    }

    fun debug(message: Any) {
        log.debug { appendExecutionInfo(message.toString()) }
    }

    fun trace(message: Any) {
        log.trace { appendExecutionInfo(message.toString()) }
    }

    private fun appendExecutionInfo(message: String): String {
        return "\n|businessKey: ${execution.businessKey}" +
            "\n|activity: ${execution.currentActivityId} ${execution.currentActivityName}" +
            "\n|processDefId: ${execution.processDefinitionId}, processInstId: ${execution.processInstanceId}" +
            "\n|Message:" +
            "\n$message"
    }
}
