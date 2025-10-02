package ru.citeck.ecos.process.domain.procdef.dto

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import java.time.Instant
import java.util.*

@Component
class ProcDefRevDataProvider(
    private val procDefRevRepo: ProcDefRevRepository
) {

    fun getData(dto: ProcDefRevDto): ByteArray {
        val revId = EntityUuid(0, dto.id)
        val revEntity = procDefRevRepo.findById(revId)
            ?: throw IllegalStateException("ProcDefRevEntity not found by id: $revId")
        return revEntity.data!!
    }
}

data class ProcDefRevDto(
    var id: UUID,

    var format: String,

    var image: ByteArray? = null,

    var procDefId: String,

    var workspace: String,

    var comment: String = "",

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
    private val _data: Lazy<ByteArray> = lazy { initialData ?: dataProvider!!.getData(this) }

    @Transient
    private var dataProvider: ProcDefRevDataProvider? = null

    fun loadData(dataProvider: ProcDefRevDataProvider): ByteArray {
        this.dataProvider = dataProvider
        return _data.value
    }
}

enum class ProcDefRevDataState {
    RAW,
    CONVERTED;

    companion object {

        fun from(value: String?): ProcDefRevDataState = if (value.isNullOrBlank()) {
            CONVERTED
        } else {
            valueOf(value)
        }
    }
}
