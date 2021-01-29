package ru.citeck.ecos.process.domain.procdef.api.rest

import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService

@Component
@RestController
@RequestMapping("/api/procdef")
class ProcDefController(
    val procDefService: ProcDefService
) {

    @GetMapping("cache")
    fun getCache(): String {
        return procDefService.getCacheKey()
    }
}
