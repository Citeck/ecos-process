package ru.citeck.ecos.process.domain.proc.repo.edata

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.common.EcosDataAbstractAdapter
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcInstanceRepository
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity
import ru.citeck.ecos.process.domain.proc.repo.ProcessStateEntity
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

class EcosDataProcInstanceAdapter(
    recordsService: RecordsService,
    val procStateAdapter: EcosDataProcStateAdapter
) : ProcInstanceRepository, EcosDataAbstractAdapter<EcosDataProcInstanceAdapter.ProcInstanceAtts>(
    recordsService,
    SRC_ID,
    mapOf(
        "created" to RecordConstants.ATT_CREATED,
        "createdBy" to RecordConstants.ATT_CREATOR,
        "modified" to RecordConstants.ATT_MODIFIED,
        "modifiedBy" to RecordConstants.ATT_MODIFIER
    ),
    ProcInstanceAtts::class
) {

    companion object {
        const val SRC_ID = EcosDataProcStateConfig.PROC_INSTANCE_REPO_SRC_ID

        const val ATT_ID = "id"

        fun toRef(entity: ProcessInstanceEntity?): EntityRef {
            val localId = entity?.id?.id?.toString() ?: return EntityRef.EMPTY
            return EntityRef.create(AppName.EPROC, SRC_ID, localId)
        }
    }

    init {
        procStateAdapter.procInstanceAdapter = this
    }

    fun getByRef(ref: EntityRef): ProcessInstanceEntity? {
        return getByRefRaw(ref)?.convertToEntity()
    }

    override fun findById(id: EntityUuid): ProcessInstanceEntity? {
        return getByIdRaw(id)?.convertToEntity()
    }

    override fun save(entity: ProcessInstanceEntity): ProcessInstanceEntity {
        val mutation = RecordAtts(EntityRef.create(SRC_ID, ""))
        val atts = ObjectData.create(entity)
        if (entity.id != null) {
            atts[ATT_ID] = entity.id!!.id.toString()
        } else {
            atts.remove(ATT_ID)
        }
        mutation.setAtts(atts)
        // permissions should be checked before
        val idAfterMutation = AuthContext.runAsSystem {
            recordsService.mutate(mutation)
        }
        return entity.copyWithId(idAfterMutation.getLocalId())
    }

    private fun ProcInstanceAtts.convertToEntity(): ProcessInstanceEntity {
        val entity = WrappedProcInstanceEntity(state)
        entity.id = EntityUuid(0, UUID.fromString(id))
        entity.procType = procType
        entity.recordRef = recordRef?.toString()
        entity.created = created
        entity.modified = modified
        return entity
    }

    class ProcInstanceAtts(
        var id: String,
        var procType: String?,
        var recordRef: EntityRef?,
        var state: EntityRef?,
        @AttName(RecordConstants.ATT_CREATED)
        var created: Instant?,
        @AttName(RecordConstants.ATT_MODIFIED)
        var modified: Instant?
    )

    inner class WrappedProcInstanceEntity(
        private val stateRef: EntityRef?
    ) : ProcessInstanceEntity() {

        private val lazyState: ProcessStateEntity? by lazy {
            stateRef?.let {
                val entity = procStateAdapter.getByRef(it)
                super.state = entity
                entity?.process = this
                entity
            }
        }

        override var state: ProcessStateEntity?
            get() = super.state ?: lazyState
            set(value) {
                super.state = value
            }
    }
}
