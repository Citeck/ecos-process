package ru.citeck.ecos.process.domain.bpmn.api.rest

import org.springframework.http.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/proc-def")
class ProcessDefVersionController(
    private val recordsService: RecordsService
) {

    @GetMapping("/version/data", produces = [MediaType.APPLICATION_XML_VALUE])
    fun getVersionData(
        @RequestParam(required = true) ref: RecordRef
    ): HttpEntity<ByteArray> {

        val versionData = recordsService.getAtts(ref, VersionData::class.java)

        val headers = HttpHeaders()
        headers.setCacheControl(
            CacheControl.maxAge(4, TimeUnit.HOURS)
                .mustRevalidate()
                .cachePublic()
        )
        headers.contentDisposition = ContentDisposition.builder("attachment")
            .filename(versionData.fileName)
            .build()

        return HttpEntity<ByteArray>(versionData.data, headers)
    }

    private data class VersionData(
        val data: ByteArray,
        val fileName: String
    )
}
