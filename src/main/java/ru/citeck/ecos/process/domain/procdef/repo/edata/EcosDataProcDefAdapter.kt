package ru.citeck.ecos.process.domain.procdef.repo.edata

import org.springframework.data.domain.*
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.common.EcosDataAbstractAdapter
import ru.citeck.ecos.process.domain.bpmn.DEFAULT_BPMN_SECTION
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRepository
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.Predicates
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.SortBy
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*

class EcosDataProcDefAdapter(
    recordsService: RecordsService,
    val ecosDataProcDefRevAdapter: EcosDataProcDefRevAdapter,
    workspaceService: WorkspaceService
) : ProcDefRepository, EcosDataAbstractAdapter<EcosDataProcDefAdapter.ProcDefRecordAtts>(
    recordsService,
    SRC_ID,
    mapOf(
        "created" to RecordConstants.ATT_CREATED,
        "modified" to RecordConstants.ATT_MODIFIED,
        "moduleId" to ATT_EXT_ID
    ),
    ProcDefRecordAtts::class,
    workspaceService
) {

    companion object {
        private const val SRC_ID = EcosDataProcDefConfig.PROC_DEF_REPO_SRC_ID

        private const val ATT_ID = "id"
        private const val ATT_EXT_ID = "extId"
        private const val ATT_PROC_TYPE = "procType"
        private const val ATT_ALF_TYPE = "alfType"
        private const val ATT_ECOS_TYPE_REF = "ecosTypeRef"
        private const val ATT_ENABLED = "enabled"
        private const val ATT_SECTION_REF = "sectionRef"

        fun toRef(entity: ProcDefEntity?): EntityRef {
            val localId = entity?.id?.id?.toString() ?: return EntityRef.EMPTY
            return EntityRef.create(AppName.EPROC, SRC_ID, localId)
        }
    }

    init {
        ecosDataProcDefRevAdapter.ecosDataProcDefAdapter = this
        registerValuePredMapping(ATT_SECTION_REF) { predicate ->
            val sectionRef = predicate.getValue().asText()
            if (sectionRef == DEFAULT_BPMN_SECTION) {
                Predicates.or(
                    predicate,
                    Predicates.empty(ATT_SECTION_REF)
                )
            } else {
                predicate
            }
        }
    }

    fun getByRef(ref: EntityRef): ProcDefEntity? {
        return getByRefRaw(ref)?.convertToEntity()
    }

    override fun delete(entity: ProcDefEntity) {
        // permissions should be checked before
        AuthContext.runAsSystem {
            recordsService.delete(toRef(entity))
        }
    }

    override fun save(entity: ProcDefEntity): ProcDefEntity {
        val mutation = RecordAtts(EntityRef.create(SRC_ID, ""))
        val atts = ObjectData.create(entity)
        if (entity.id != null) {
            atts[ATT_ID] = entity.id!!.id.toString()
        } else {
            mutation[ATT_EXT_ID] = atts[ATT_ID]
            atts.remove(ATT_ID)
        }
        mutation.setAtts(atts)
        // permissions should be checked before
        val idAfterMutation = AuthContext.runAsSystem {
            recordsService.mutate(mutation)
        }
        return entity.copyWithId(idAfterMutation.getLocalId())
    }

    override fun findAll(workspaces: List<String>, predicate: Predicate, pageable: Pageable): Page<ProcDefEntity> {
        return findAllRaw(workspaces, predicate, pageable).map { it.convertToEntity() }
    }

    override fun findFirstEnabledByEcosType(
        workspace: String,
        type: String,
        ecosTypeRef: String
    ): ProcDefEntity? {
        val predicate = Predicates.and(
            Predicates.eq(ATT_PROC_TYPE, type),
            Predicates.eq(ATT_ECOS_TYPE_REF, ecosTypeRef),
            Predicates.eq(ATT_ENABLED, true)
        )
        return findAllRaw(workspace, predicate, 0, 1)
            .getRecords()
            .firstOrNull()
            ?.convertToEntity()
    }

    override fun findByIdInWs(workspace: String, type: String, extId: String): ProcDefEntity? {
        return findAllRaw(
            workspace,
            Predicates.and(
                Predicates.eq(ATT_PROC_TYPE, type),
                Predicates.eq(ATT_EXT_ID, extId)
            ),
            0, 1
        ).getRecords().firstOrNull()?.convertToEntity()
    }

    override fun getCount(workspaces: List<String>, predicate: Predicate): Long {
        return findAllRaw(workspaces, predicate, 0, 0).getTotalCount()
    }

    override fun getCount(workspaces: List<String>): Long {
        return getCount(workspaces, Predicates.alwaysTrue())
    }

    override fun getLastModifiedDate(): Instant {
        return findAllRaw(
            emptyList(),
            Predicates.alwaysTrue(),
            0, 1, listOf(
                SortBy(RecordConstants.ATT_MODIFIED, ascending = false)
            )
        ).getRecords().firstOrNull()?.modified ?: Instant.EPOCH
    }

    override fun findFirstByProcTypeAndAlfType(workspace: String, type: String, alfType: String): ProcDefEntity? {
        return findAllRaw(
            listOf(workspace),
            Predicates.and(
                Predicates.eq(ATT_PROC_TYPE, type),
                Predicates.eq(ATT_ALF_TYPE, alfType)
            ),
            0, 1
        ).getRecords().firstOrNull()?.convertToEntity()
    }

    private fun ProcDefRecordAtts.convertToEntity(): WrappedProcDefEntity {

        val entity = WrappedProcDefEntity(this.lastRev)
        entity.id = EntityUuid(0, UUID.fromString(this.id))

        entity.procType = this.procType
        entity.name = this.name
        entity.extId = this.extId
        entity.ecosTypeRef = this.ecosTypeRef?.toString()
        entity.formRef = this.formRef?.toString()
        entity.workingCopySourceRef = this.workingCopySourceRef?.toString()
        entity.alfType = this.alfType
        entity.created = this.created
        entity.modified = this.modified
        entity.enabled = this.enabled
        entity.autoStartEnabled = this.autoStartEnabled
        entity.autoDeleteEnabled = this.autoDeleteEnabled
        entity.sectionRef = this.sectionRef?.toString()
        entity.workspace = this.workspace

        return entity
    }

    class ProcDefRecordAtts(
        var id: String,
        var procType: String,
        var name: String?,
        var extId: String,
        var ecosTypeRef: EntityRef?,
        var formRef: EntityRef?,
        var workingCopySourceRef: EntityRef?,
        var alfType: String?,
        @AttName(RecordConstants.ATT_CREATED)
        var created: Instant?,
        @AttName(RecordConstants.ATT_MODIFIED)
        var modified: Instant?,
        var enabled: Boolean?,
        var autoStartEnabled: Boolean?,
        var autoDeleteEnabled: Boolean?,
        var sectionRef: EntityRef?,
        var lastRev: EntityRef?,
        @AttName("_workspace?localId!")
        var workspace: String
    )

    inner class WrappedProcDefEntity(
        private val lastRevRef: EntityRef?
    ) : ProcDefEntity() {

        private val lazyLastProcDef: ProcDefRevEntity? by lazy {
            lastRevRef?.let {
                val entity = ecosDataProcDefRevAdapter.getByRef(it)
                super.lastRev = entity
                entity?.processDef = this
                entity
            }
        }

        override var lastRev: ProcDefRevEntity?
            get() = super.lastRev ?: lazyLastProcDef
            set(value) {
                super.lastRev = value
            }
    }
}
