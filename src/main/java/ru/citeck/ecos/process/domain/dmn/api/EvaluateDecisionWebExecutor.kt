package ru.citeck.ecos.process.domain.dmn.api

import org.springframework.stereotype.Component
import ru.citeck.ecos.model.lib.computed.ComputeDmnDecisionRequestDto
import ru.citeck.ecos.model.lib.computed.ComputeDmnDecisionResponseDto
import ru.citeck.ecos.model.lib.computed.ComputeDmnDecisionWebApi
import ru.citeck.ecos.process.common.toDecisionKey
import ru.citeck.ecos.process.domain.dmn.service.EcosDmnService
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutor
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorReq
import ru.citeck.ecos.webapp.api.web.executor.EcosWebExecutorResp

@Component
class EvaluateDecisionWebExecutor(
    private val ecosDmnService: EcosDmnService
) : EcosWebExecutor {

    override fun execute(request: EcosWebExecutorReq, response: EcosWebExecutorResp) {

        val requestData = request.getBodyReader().readDto(ComputeDmnDecisionRequestDto::class.java)
        if (requestData.decisionRef.isEmpty()) {
            error("Decision ref can't be empty")
        }

        val evalResult = ecosDmnService.evaluateDecisionByKeyAndCollectMapEntries(
            requestData.decisionRef.toDecisionKey(),
            requestData.variables
        )

        response.getBodyWriter().writeDto(ComputeDmnDecisionResponseDto(evalResult))
    }

    override fun getPath(): String {
        return ComputeDmnDecisionWebApi.PATH
    }

    override fun isReadOnly(): Boolean {
        return true
    }
}
