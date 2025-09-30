package ru.citeck.ecos.process.domain.procdef.repo.edata

import org.springframework.data.domain.*
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.common.EcosDataAbstractAdapter
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevRepository
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

open class EcosDataProcDefRevAdapter(
    recordsService: RecordsService
) : ProcDefRevRepository, EcosDataAbstractAdapter<EcosDataProcDefRevAdapter.ProcDefRevRecordAtts>(
    recordsService,
    SRC_ID,
    mapOf(
        "created" to RecordConstants.ATT_CREATED,
        "createdBy" to RecordConstants.ATT_CREATOR,
        "modified" to RecordConstants.ATT_MODIFIED,
        "modifiedBy" to RecordConstants.ATT_MODIFIER
    ),
    ProcDefRevRecordAtts::class
) {

    companion object {

        private const val SRC_ID = EcosDataProcDefConfig.PROC_DEF_REV_REPO_SRC_ID

        private const val ATT_ID = "id"
        private const val ATT_PROCESS_DEF = "processDef"
        private const val ATT_DEPLOYMENT_ID = "deploymentId"

        fun toRef(entity: ProcDefRevEntity?): EntityRef {
            val localId = entity?.id?.id?.toString() ?: return EntityRef.EMPTY
            return EntityRef.create(AppName.EPROC, SRC_ID, localId)
        }
    }

    lateinit var ecosDataProcDefAdapter: EcosDataProcDefAdapter

    fun getByRef(ref: EntityRef): ProcDefRevEntity? {
        return getByRefRaw(ref)?.convertToEntity()
    }

    override fun save(entity: ProcDefRevEntity): ProcDefRevEntity {
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

    override fun findById(id: EntityUuid): ProcDefRevEntity? {
        return findAllRaw(
            Predicates.eq(ATT_ID, id.id),
            0, 1
        ).getRecords().firstOrNull()?.convertToEntity()
    }

    override fun findAllById(ids: Iterable<EntityUuid>): List<ProcDefRevEntity> {
        return findAllRaw(
            Predicates.inVals(ATT_ID, ids.mapTo(ArrayList()) { it.id }),
            0, 50_000
        ).getRecords().map { it.convertToEntity() }
    }

    override fun findAllByProcessDef(processDef: ProcDefEntity): List<ProcDefRevEntity> {
        return findAllRaw(
            Predicates.eq(ATT_PROCESS_DEF, EcosDataProcDefAdapter.toRef(processDef)),
            0, 50_000
        ).getRecords().map { it.convertToEntity() }
    }

    override fun findByDeploymentId(deploymentId: String): ProcDefRevEntity? {
        return findAllRaw(
            Predicates.eq(ATT_DEPLOYMENT_ID, deploymentId),
            0, 1
        ).getRecords().firstOrNull()?.convertToEntity()
    }

    override fun findByDeploymentIdIsIn(deploymentIds: List<String>): List<ProcDefRevEntity> {
        return findAllRaw(
            Predicates.inVals(ATT_DEPLOYMENT_ID, deploymentIds),
            0, 50_000
        ).getRecords().map { it.convertToEntity() }
    }

    override fun queryAllByDeploymentIdIsNotNull(pageable: Pageable): Slice<ProcDefRevEntity> {
        return findAll(Predicates.alwaysTrue(), pageable)
    }

    override fun deleteAll(entities: List<ProcDefRevEntity>) {
        // permissions should be checked before
        AuthContext.runAsSystem {
            recordsService.delete(entities.map { toRef(it) })
        }
    }

    fun findAll(predicate: Predicate, pageable: Pageable): Page<ProcDefRevEntity> {
        return findAllRaw(predicate, pageable).map { it.convertToEntity() }
    }

    private fun ProcDefRevRecordAtts.convertToEntity(): WrappedProcDefRevEntity {

        val entity = WrappedProcDefRevEntity(
            processDefRef = processDef,
            prevRevRef = prevRev
        )
        entity.id = EntityUuid(0, UUID.fromString(this.id))
        entity.format = format
        entity.data = data
        entity.created = created ?: Instant.EPOCH
        entity.createdBy = creator?.getLocalId()
        entity.deploymentId = deploymentId
        entity.dataState = dataState
        entity.comment = comment
        entity.version = version
        entity.image = image

        return entity
    }

    class ProcDefRevRecordAtts(
        var id: String,
        var format: String?,
        var data: ByteArray?,
        var processDef: EntityRef?,
        @AttName(RecordConstants.ATT_CREATED)
        var created: Instant?,
        @AttName(RecordConstants.ATT_CREATOR)
        var creator: EntityRef?,
        var deploymentId: String?,
        var dataState: String?,
        var comment: String?,
        var version: Int,
        var prevRev: EntityRef?,
        var image: ByteArray?
    )

    inner class WrappedProcDefRevEntity(
        private val processDefRef: EntityRef?,
        private val prevRevRef: EntityRef?
    ) : ProcDefRevEntity() {

        private val lazyPrevRev: ProcDefRevEntity? by lazy {
            prevRevRef?.let {
                val entity = getByRef(prevRevRef)
                super.prevRev = entity
                entity
            }
        }

        private val lazyProcessDef: ProcDefEntity? by lazy {
            processDefRef?.let {
                val entity = ecosDataProcDefAdapter.getByRef(it)
                super.processDef = entity
                entity
            }
        }

        override var processDef: ProcDefEntity?
            get() = super.processDef ?: lazyProcessDef
            set(value) {
                super.processDef = value
            }

        override var prevRev: ProcDefRevEntity?
            get() = super.prevRev ?: lazyPrevRev
            set(value) {
                super.prevRev = value
            }
    }
}
