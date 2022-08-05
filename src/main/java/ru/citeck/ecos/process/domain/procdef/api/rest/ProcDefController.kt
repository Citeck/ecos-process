package ru.citeck.ecos.process.domain.procdef.api.rest

import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.service.ProcDefService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Component
@RestController
@RequestMapping("/api/procdef")
class ProcDefController(
    val procDefService: ProcDefService
) : ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext
    private lateinit var noPreviewData: ByteArray

    @PostConstruct
    fun init() {
        noPreviewData = applicationContext.getResource("classpath:proc/no-preview.svg").inputStream.use {
            it.readBytes()
        }
    }

    @GetMapping("cache")
    fun getCache(): String {
        return procDefService.getCacheKey()
    }

    @GetMapping("preview", produces = ["image/svg+xml"])
    fun getImage(
        @RequestParam(required = false) ref: String? = null
    ): HttpEntity<ByteArray> {

        val headers = HttpHeaders()
        headers.setCacheControl(
            CacheControl.maxAge(4, TimeUnit.HOURS)
                .mustRevalidate()
                .cachePublic()
        )
        return HttpEntity<ByteArray>(getPreviewData(ref.toEntityRef()), headers)
    }

    private fun getPreviewData(ref: EntityRef): ByteArray {

        if (ref.getLocalId().isEmpty()) {
            return noPreviewData
        }

        val procType = when (ref.getSourceId()) {
            "bpmn-def" -> "bpmn"
            "cmmn-def" -> "cmmn"
            else -> error("Unknown ref: $ref")
        }
        val procDefRef = ProcDefRef.create(procType, ref.getLocalId())
        val procDef = procDefService.getProcessDefById(procDefRef) ?: return noPreviewData

        val lastrev = procDefService.getProcessDefRev(procType, procDef.revisionId)
        return lastrev?.image ?: noPreviewData
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}
