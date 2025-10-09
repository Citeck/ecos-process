package ru.citeck.ecos.process.domain.bpmn.api.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutService
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp


@Component
class BpmnAutoLayoutWebExecutor(
    private val bpmnAutoLayoutService: BpmnAutoLayoutService
) : EcosWebExecutor {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    override fun execute(
        request: EcosWebExecutorReq,
        response: EcosWebExecutorResp
    ) {
        val requestDto = request.getBodyReader().readDto(BpmnAutoLayoutRequest::class.java)
        val result = applyAutoLayout(requestDto)
        response.getBodyWriter().writeDto(result)
    }

    private fun applyAutoLayout(request: BpmnAutoLayoutRequest): BpmnAutoLayoutResponse {
        log.debug { "Received request to apply auto-layout to BPMN definition" }

        if (request.definition.isBlank()) {
            return BpmnAutoLayoutResponse(
                success = false,
                definition = "",
                message = "BPMN definition cannot be empty"
            )
        }

        if (!isValidBpmnXml(request.definition)) {
            return BpmnAutoLayoutResponse(
                success = false,
                definition = "",
                message = "Invalid BPMN XML format. Definition must be valid BPMN XML"
            )
        }

        return try {
            val transformedDefinition = bpmnAutoLayoutService.applyAutoLayout(request.definition)

            log.debug { "Successfully applied auto-layout to BPMN definition" }

            BpmnAutoLayoutResponse(
                success = true,
                definition = transformedDefinition,
                message = "Layout applied successfully"
            )
        } catch (e: Exception) {
            log.error(e) { "Failed to apply auto-layout to BPMN definition" }

            BpmnAutoLayoutResponse(
                success = false,
                definition = "",
                message = "Failed to apply auto-layout: ${e.message}"
            )
        }
    }

    private fun isValidBpmnXml(definition: String): Boolean {
        val trimmed = definition.trim()
        return trimmed.startsWith("<?xml") ||
            trimmed.contains("<definitions") ||
            trimmed.contains("<bpmn:definitions")
    }

    override fun getPath(): String {
        return "/bpmn/auto-layout/transform"
    }

    override fun isReadOnly(): Boolean {
        return true
    }
}

data class BpmnAutoLayoutRequest(
    val definition: String
)

data class BpmnAutoLayoutResponse(
    val success: Boolean,
    val definition: String,
    val message: String
)
