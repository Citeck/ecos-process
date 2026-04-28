package ru.citeck.ecos.process.domain.bpmn.api.rest

import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutRequest
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutResponse
import ru.citeck.ecos.process.domain.bpmn.io.BpmnAutoLayoutService

@Component
@RestController
@RequestMapping("/api/bpmn/auto-layout")
class BpmnAutoLayoutController(
    private val bpmnAutoLayoutService: BpmnAutoLayoutService
) {

    @PostMapping("/transform")
    fun transform(@RequestBody request: BpmnAutoLayoutRequest): BpmnAutoLayoutResponse {
        return bpmnAutoLayoutService.applyAutoLayout(request)
    }
}
