package ru.citeck.ecos.process.domain.proc.repo.edata

import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.data.sql.records.DbRecordsControlAtts
import ru.citeck.ecos.process.common.EcosDataAbstractAdapter
import ru.citeck.ecos.process.common.patch.MongoToEcosDataMigrationConfig
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity
import ru.citeck.ecos.process.domain.proc.repo.ProcessStateEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.edata.EcosDataProcDefRevAdapter
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

class EcosDataProcStateAdapter(
    recordsService: RecordsService,
    val procDefRevAdapter: EcosDataProcDefRevAdapter
) : ProcStateRepository, EcosDataAbstractAdapter<EcosDataProcStateAdapter.ProcStateAtts>(
    recordsService,
    SRC_ID,
    mapOf(
        "created" to RecordConstants.ATT_CREATED,
        "createdBy" to RecordConstants.ATT_CREATOR,
        "modified" to RecordConstants.ATT_MODIFIED,
        "modifiedBy" to RecordConstants.ATT_MODIFIER
    ),
    ProcStateAtts::class
) {

    companion object {
        const val SRC_ID = EcosDataProcStateConfig.PROC_STATE_REPO_SRC_ID

        const val ATT_ID = "id"

        fun toRef(entity: ProcessStateEntity?): EntityRef {
            val localId = entity?.id?.id?.toString() ?: return EntityRef.EMPTY
            return EntityRef.create(AppName.EPROC, SRC_ID, localId)
        }
    }

    lateinit var procInstanceAdapter: EcosDataProcInstanceAdapter

    fun getByRef(ref: EntityRef): ProcessStateEntity? {
        return getByRefRaw(ref)?.convertToEntity()
    }

    override fun findFirstByProcDefRevIn(procDefRev: List<ProcDefRevEntity>): ProcessStateEntity? {
        val procDefRefs = procDefRev.map { EcosDataProcDefRevAdapter.toRef(it) }
        return findAllRaw(
            emptyList(),
            predicate = Predicates.inVals("procDefRev", procDefRefs),
            skipCount = 0,
            maxItems = 1
        ).getRecords().firstOrNull()?.convertToEntity()
    }

    override fun findById(id: EntityUuid): ProcessStateEntity? {
        return getByIdRaw(id)?.convertToEntity()
    }

    override fun save(entity: ProcessStateEntity): ProcessStateEntity {
        val mutation = RecordAtts(EntityRef.create(SRC_ID, ""))
        val atts = ObjectData.create(entity)
        if (entity.id != null) {
            atts[ATT_ID] = entity.id!!.id.toString()
        } else {
            atts.remove(ATT_ID)
        }
        if (MongoToEcosDataMigrationConfig.isMigrationContext()) {
            atts[DbRecordsControlAtts.DISABLE_AUDIT] = true
            atts[DbRecordsControlAtts.DISABLE_EVENTS] = true
        }
        mutation.setAtts(atts)
        // permissions should be checked before
        val idAfterMutation = AuthContext.runAsSystem {
            recordsService.mutate(mutation)
        }
        return entity.copyWithId(idAfterMutation.getLocalId())
    }

    private fun ProcStateAtts.convertToEntity(): ProcessStateEntity {
        val entity = WrappedStateEntity(process, procDefRev)
        entity.id = EntityUuid(0, UUID.fromString(id))
        entity.data = data
        entity.created = created
        entity.version = version
        return entity
    }

    class ProcStateAtts(
        var id: String,
        var data: ByteArray?,
        var process: EntityRef?,
        var procDefRev: EntityRef?,
        @AttName(RecordConstants.ATT_CREATED)
        var created: Instant,
        var version: Int
    )

    inner class WrappedStateEntity(
        private val processRef: EntityRef?,
        private val procDefRevRef: EntityRef?
    ) : ProcessStateEntity() {

        private val lazyProcess: ProcessInstanceEntity? by lazy {
            processRef?.let {
                val entity = procInstanceAdapter.getByRef(it)
                super.process = entity
                entity
            }
        }

        private val lazyProcessDefRev: ProcDefRevEntity? by lazy {
            procDefRevRef?.let {
                val entity = procDefRevAdapter.getByRef(it)
                super.procDefRev = entity
                entity
            }
        }

        override var process: ProcessInstanceEntity?
            get() = super.process ?: lazyProcess
            set(value) {
                super.process = value
            }

        override var procDefRev: ProcDefRevEntity?
            get() = super.procDefRev ?: lazyProcessDefRev
            set(value) {
                super.procDefRev = value
            }
    }
}
