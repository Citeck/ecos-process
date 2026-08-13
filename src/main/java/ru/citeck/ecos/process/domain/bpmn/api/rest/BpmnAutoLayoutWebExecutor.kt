package ru.citeck.ecos.process.domain.bpmn.api.rest

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutRequest
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutService
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp

@Component
class BpmnAutoLayoutWebExecutor(
    private val bpmnAutoLayoutService: BpmnAutoLayoutService
) : EcosWebExecutor {

    override fun execute(
        request: EcosWebExecutorReq,
        response: EcosWebExecutorResp
    ) {
        val requestDto = request.getBodyReader().readDto(BpmnAutoLayoutRequest::class.java)
        val result = bpmnAutoLayoutService.applyAutoLayout(requestDto)
        response.getBodyWriter().writeDto(result)
    }

    override fun getPath(): String {
        return "/bpmn/auto-layout/transform"
    }

    override fun isReadOnly(): Boolean {
        return true
    }
}
