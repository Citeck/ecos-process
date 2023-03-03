package ru.citeck.ecos.process.domain.procdef.service

import com.querydsl.core.types.dsl.BooleanExpression
import lombok.Data
import lombok.RequiredArgsConstructor
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefRecords
import ru.citeck.ecos.process.domain.cmmn.api.records.CmmnProcDefRecords
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.dmn.DMN_PROC_TYPE
import ru.citeck.ecos.process.domain.dmn.api.records.DMN_DEF_SOURCE_ID
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository
import ru.citeck.ecos.process.domain.procdef.convert.toDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEvent
import ru.citeck.ecos.process.domain.procdef.events.ProcDefEventEmitter
import ru.citeck.ecos.process.domain.procdef.repo.*
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.system.measureTimeMillis

@Service
@RequiredArgsConstructor
class ProcDefServiceImpl(
    private val procDefRepo: ProcDefRepository,
    private val procDefRevRepo: ProcDefRevRepository,
    private val procStateRepo: ProcStateRepository,
    private val tenantService: ProcTenantService,
    private val recordsService: RecordsService,
    private val procDefEventEmitter: ProcDefEventEmitter
) : ProcDefService {

    companion object {
        private val STARTUP_TIME_STR = Instant.now().toEpochMilli().toString()
        private const val FIRST_VERSION = 1.0

        private val log = KotlinLogging.logger {}
    }

    private val procDefListeners = ConcurrentHashMap<String, MutableList<(ProcDefDto) -> Unit>>()

    override fun uploadProcDef(processDef: NewProcessDefDto): ProcDefDto {
        val procDefDto = uploadProcDefImpl(processDef)
        procDefListeners[procDefDto.procType]?.forEach { it.invoke(procDefDto) }
        return procDefDto
    }

    private fun uploadProcDefImpl(processDef: NewProcessDefDto): ProcDefDto {

        val currentTenant = tenantService.getCurrent()
        val now = Instant.now()

        var currentProcDef = procDefRepo.findFirstByIdTntAndProcTypeAndExtId(
            currentTenant,
            processDef.procType,
            processDef.id
        )

        var newRevision = ProcDefRevEntity()

        newRevision.id = EntityUuid(tenantService.getCurrent(), UUID.randomUUID())
        newRevision.created = now
        newRevision.data = processDef.data
        newRevision.image = processDef.image
        newRevision.format = processDef.format
        newRevision.createdBy = AuthContext.getCurrentUser()

        val sendProcDefEvent: () -> Unit

        if (currentProcDef == null) {

            currentProcDef = ProcDefEntity()
            currentProcDef.id = EntityUuid(tenantService.getCurrent(), UUID.randomUUID())
            currentProcDef.alfType = processDef.alfType
            currentProcDef.ecosTypeRef = processDef.ecosTypeRef.toString()
            currentProcDef.formRef = processDef.formRef.toString()
            currentProcDef.extId = processDef.id
            currentProcDef.procType = processDef.procType
            currentProcDef.name = mapper.toString(processDef.name)
            currentProcDef.modified = now
            currentProcDef.created = now
            currentProcDef.enabled = processDef.enabled
            currentProcDef.autoStartEnabled = processDef.autoStartEnabled
            currentProcDef.sectionRef = processDef.sectionRef.toString()

            currentProcDef = procDefRepo.save<ProcDefEntity>(currentProcDef)
            newRevision.version = 0

            sendProcDefEvent = {
                procDefEventEmitter.emitProcDefCreate(
                    createEvent(processDef.procType, processDef.id, FIRST_VERSION, processDef.createdFromVersion)
                )
            }
        } else {

            currentProcDef.alfType = processDef.alfType
            currentProcDef.ecosTypeRef = processDef.ecosTypeRef.toString()
            currentProcDef.formRef = processDef.formRef.toString()
            currentProcDef.name = mapper.toString(processDef.name)
            currentProcDef.modified = now
            currentProcDef.enabled = processDef.enabled
            currentProcDef.autoStartEnabled = processDef.autoStartEnabled
            currentProcDef.sectionRef = processDef.sectionRef.toString()

            newRevision.version = currentProcDef.lastRev!!.version + 1
            newRevision.prevRev = currentProcDef.lastRev

            sendProcDefEvent = {
                procDefEventEmitter.emitProcDefUpdate(
                    createEvent(
                        procType = currentProcDef!!.procType!!,
                        currentProcDef!!.extId!!,
                        newRevision.version.toDouble().inc(),
                        processDef.createdFromVersion
                    )
                )
            }
        }

        newRevision.processDef = currentProcDef
        newRevision = procDefRevRepo.save(newRevision)

        currentProcDef.lastRev = newRevision
        currentProcDef = procDefRepo.save(currentProcDef)

        sendProcDefEvent.invoke()

        return currentProcDef.toDto()
    }

    private fun createEvent(
        procType: String,
        id: String,
        version: Double,
        createdFromVersion: EntityRef
    ): ProcDefEvent {
        return ProcDefEvent(
            procDefRef = when (procType) {
                BPMN_PROC_TYPE -> RecordRef.create(AppName.EPROC, BpmnProcDefRecords.SOURCE_ID, id)
                DMN_PROC_TYPE -> RecordRef.create(AppName.EPROC, DMN_DEF_SOURCE_ID, id)
                CmmnProcDefRecords.CMMN_PROC_TYPE -> {
                    RecordRef.create(AppName.EPROC, CmmnProcDefRecords.SOURCE_ID, id)
                }

                else -> throw IllegalArgumentException("Unknown proc type: $procType")
            },
            version = version,
            createdFromVersion = let {
                if (createdFromVersion == EntityRef.EMPTY) {
                    0.0
                } else {
                    recordsService.getAtt(
                        createdFromVersion,
                        "version"
                    ).asDouble(
                        0.0
                    )
                }
            }
        )
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int): List<ProcDefDto> {
        return findAllProcDefEntities(predicate, max, skip)
            .map { entity: ProcDefEntity -> entity.toDto() }
    }

    override fun findAllWithData(predicate: Predicate?, max: Int, skip: Int): List<ProcDefWithDataDto> {
        return findAllProcDefEntities(predicate ?: VoidPredicate.INSTANCE, max, skip).map { entity: ProcDefEntity ->
            val procDefDto = entity.toDto()
            val procDefRevDto = entity.lastRev!!.toDto()
            ProcDefWithDataDto(procDefDto, procDefRevDto)
        }
    }

    private fun findAllProcDefEntities(predicate: Predicate, max: Int, skip: Int): List<ProcDefEntity> {
        val page = PageRequest.of(skip / max, max, Sort.by(Sort.Order.desc("created")))
        val query = predicateToQuery(predicate)
        return procDefRepo.findAll(query, page).content
    }

    private fun predicateToQuery(predicate: Predicate): BooleanExpression {

        val predQuery = PredicateUtils.convertToDto(predicate, PredicateQuery::class.java)
        val entity = QProcDefEntity.procDefEntity
        var query = entity.id.tnt.eq(tenantService.getCurrent())

        if (StringUtils.isNotBlank(predQuery.procType)) {
            query = query.and(QProcDefEntity.procDefEntity.procType.eq(predQuery.procType))
        }
        if (StringUtils.isNotBlank(predQuery.moduleId)) {
            query = query.and(QProcDefEntity.procDefEntity.extId.likeIgnoreCase("%" + predQuery.moduleId + "%"))
        }
        return query
    }

    override fun getCount(): Long {
        return procDefRepo.getCount(tenantService.getCurrent())
    }

    override fun getCount(predicate: Predicate): Long {
        return procDefRepo.count(predicateToQuery(predicate))
    }

    override fun uploadNewRev(dto: ProcDefWithDataDto): ProcDefDto {

        val currentTenant = tenantService.getCurrent()
        val id = dto.id
        val procType = dto.procType
        val procDefEntity = procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, procType, id)

        val result: ProcDefDto

        if (procDefEntity == null) {

            val newProcessDefDto = NewProcessDefDto(
                id = dto.id,
                name = dto.name,
                data = dto.data,
                image = dto.image,
                alfType = dto.alfType,
                ecosTypeRef = dto.ecosTypeRef,
                formRef = dto.formRef,
                format = dto.format,
                procType = dto.procType,
                enabled = dto.enabled,
                autoStartEnabled = dto.autoStartEnabled,
                sectionRef = dto.sectionRef,
                createdFromVersion = dto.createdFromVersion
            )
            result = uploadProcDefImpl(newProcessDefDto)
        } else {

            val currentData = procDefEntity.lastRev!!.data

            if (!Arrays.equals(currentData, dto.data)) {

                val newProcessDefDto = NewProcessDefDto(
                    id = dto.id,
                    enabled = dto.enabled,
                    autoStartEnabled = dto.autoStartEnabled,
                    name = dto.name,
                    data = dto.data,
                    image = dto.image,
                    alfType = dto.alfType,
                    ecosTypeRef = dto.ecosTypeRef,
                    formRef = dto.formRef,
                    format = dto.format,
                    procType = dto.procType,
                    sectionRef = dto.sectionRef,
                    createdFromVersion = dto.createdFromVersion
                )
                result = uploadProcDefImpl(newProcessDefDto)
            } else {

                procDefEntity.alfType = dto.alfType
                procDefEntity.ecosTypeRef = dto.ecosTypeRef.toString()
                procDefEntity.formRef = dto.formRef.toString()
                procDefEntity.name = mapper.toString(dto.name)
                procDefEntity.enabled = dto.enabled
                procDefEntity.autoStartEnabled = dto.autoStartEnabled
                procDefEntity.modified = Instant.now()
                procDefEntity.sectionRef = dto.sectionRef.toString()

                result = procDefRepo.save(procDefEntity).toDto()
            }
        }

        procDefListeners[result.procType]?.forEach { it.invoke(result) }

        return result
    }

    override fun getProcessDefRev(procType: String, procDefRevId: UUID): ProcDefRevDto? {
        val revId = EntityUuid(tenantService.getCurrent(), procDefRevId)
        val revEntity = procDefRevRepo.findById(revId).orElse(null) ?: return null
        return revEntity.toDto()
    }

    override fun saveProcessDefRevDeploymentId(procDefRevId: UUID, deploymentId: String) {
        log.debug { "Save deployment id $deploymentId for process definition revision $procDefRevId" }

        val revId = EntityUuid(tenantService.getCurrent(), procDefRevId)
        val revEntity = procDefRevRepo.findById(revId).orElseThrow {
            error("Proc def rev with id $revId not found")
        }

        revEntity.deploymentId = deploymentId

        procDefRevRepo.save(revEntity)
    }

    override fun getProcessDefRevs(procDefRevIds: List<UUID>): List<ProcDefRevDto> {
        val ids = procDefRevIds.map { EntityUuid(tenantService.getCurrent(), it) }
        return procDefRevRepo.findAllById(ids).map { it.toDto() }
    }

    @Transactional(readOnly = true)
    override fun getProcessDefRevs(ref: ProcDefRef): List<ProcDefRevDto> {
        val tenant = tenantService.getCurrent()
        val procDef = procDefRepo.findOneByIdTntAndProcTypeAndExtId(tenant, ref.type, ref.id)
            ?: return emptyList()

        val result: List<ProcDefRevDto>
        val time = measureTimeMillis {
            result = procDefRevRepo.findAllByProcessDef(procDef)
                .map { it.toDto() }
                .sortedByDescending { it.created }
        }

        log.debug { "getProcessDefRevs: $time ms" }

        return result
    }

    override fun getProcessDefRevByDeploymentId(deploymentId: String): ProcDefRevDto? {
        return procDefRevRepo.findByDeploymentId(deploymentId)?.toDto()
    }

    override fun getProcessDefById(id: ProcDefRef): ProcDefWithDataDto? {
        val currentTenant = tenantService.getCurrent()
        val procDefEntity = procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, id.type, id.id)
        return procDefEntity?.let { def: ProcDefEntity ->
            ProcDefWithDataDto(def.toDto(), def.lastRev!!.toDto())
        }
    }

    override fun getCacheKey(): String {
        val currentTenant = tenantService.getCurrent()
        val page = PageRequest.of(0, 1, Sort.by(Sort.Order.desc("modified")))
        val modified = procDefRepo.getModifiedDate(currentTenant, page)
        return if (modified.isEmpty()) {
            ""
        } else {
            modified[0].modified?.toEpochMilli().toString()
        } + "-" + getCount() + "-" + STARTUP_TIME_STR
    }

    @Transactional(readOnly = true)
    override fun findProcDef(procType: String, ecosTypeRef: RecordRef?, alfTypes: List<String>?): ProcDefRevDto? {

        val currentTenant = tenantService.getCurrent()
        var processDef: ProcDefEntity? = null

        if (RecordRef.isNotEmpty(ecosTypeRef)) {

            val ecosType = ecosTypeRef.toString()
            processDef = procDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(
                currentTenant,
                procType,
                ecosType
            )
            if (processDef == null) {

                val typeInfo = recordsService.getAtts(ecosTypeRef, TypeParents::class.java)
                requireNotNull(typeInfo.parents) { "ECOS type parents can't be resolved" }

                for (parentRef in typeInfo.parents) {
                    val parentRefStr = parentRef.toString()
                    processDef = procDefRepo.findFirstByIdTntAndProcTypeAndEcosTypeRefAndEnabledTrue(
                        currentTenant,
                        procType,
                        parentRefStr
                    )
                    if (processDef != null) {
                        break
                    }
                }
            }
        }
        if (processDef == null && alfTypes != null) {
            for (alfType in alfTypes) {
                processDef = procDefRepo.findFirstByIdTntAndProcTypeAndAlfType(
                    currentTenant,
                    procType,
                    alfType
                )
                if (processDef != null) {
                    break
                }
            }
        }

        return processDef?.let { it.lastRev!!.toDto() }
    }

    override fun delete(ref: ProcDefRef) {

        val tenant = tenantService.getCurrent()

        val procDef = procDefRepo.findOneByIdTntAndProcTypeAndExtId(tenant, ref.type, ref.id) ?: return
        val revisions = procDefRevRepo.findAllByProcessDef(procDef)

        val procRev = procStateRepo.findFirstByProcDefRevIn(revisions)

        if (procRev != null) {
            error(
                "Process definition $ref can't be deleted. " +
                    "At least one process instance was started " +
                    "for recordRef: ${procRev.process!!.recordRef}"
            )
        }

        procDefRevRepo.deleteAll(revisions)
        procDefRepo.delete(procDef)
    }

    override fun listenChanges(type: String, action: (ProcDefDto) -> Unit) {
        val listeners = procDefListeners.computeIfAbsent(type) { CopyOnWriteArrayList() }
        listeners.add(action)
    }

    @Data
    class TypeParents(
        val parents: List<RecordRef>? = null
    )

    @Data
    class PredicateQuery {
        val moduleId: String? = null
        val procType: String? = null
    }
}
