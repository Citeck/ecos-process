package ru.citeck.ecos.process.domain.procdef.dto

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.engine.camunda.impl.events.bpmnevents.EcosEventType
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService
import java.time.Instant
import java.util.*
import javax.annotation.PostConstruct

@Component
internal class ProcDefRevDataProvider(
    private val procDefRevRepo: ProcDefRevRepository,
    private val tenantService: ProcTenantService
) {

    fun getData(dto: ProcDefRevDto): ByteArray {
        val revId = EntityUuid(tenantService.getCurrent(), dto.id)
        val revEntity = procDefRevRepo.findById(revId).orElseThrow {
            IllegalStateException("ProcDefRevEntity not found by id: $revId")
        }
        return revEntity.data!!
    }

    @PostConstruct
    private fun init() {
        dataProvider = this
    }
}

private lateinit var dataProvider: ProcDefRevDataProvider

data class ProcDefRevDto(
    var id: UUID,

    var format: String,

    var image: ByteArray? = null,

    var procDefId: String,

    var created: Instant,
    var createdBy: String? = null,
    var deploymentId: String? = null,

    var dataState: ProcDefRevDataState,

    var version: Int = 0,

    val initialData: ByteArray? = null
) {

    /**
     *  Lazy load data to avoid memory leaks
     */
    val data: ByteArray by lazy { initialData ?: dataProvider.getData(this) }

    override fun toString(): String {
        return "ProcDefRevDto(id=$id, format=$format, image=${image?.contentToString()}, procDefId=$procDefId, " +
            "created=$created, createdBy=$createdBy, deploymentId=$deploymentId, version=$version)"
    }
}

enum class ProcDefRevDataState {
    RAW, CONVERTED;

    companion object {

        fun from(value: String?): ProcDefRevDataState = if (value.isNullOrBlank()) {
            CONVERTED
        } else {
            valueOf(value)
        }

    }
}
