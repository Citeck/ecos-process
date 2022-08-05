package ru.citeck.ecos.process.domain.procdef.service

import com.querydsl.core.types.dsl.BooleanExpression
import lombok.Data
import lombok.RequiredArgsConstructor
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json.mapper
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.common.repo.EntityUuid
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.process.domain.proc.repo.ProcStateRepository
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRef
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefWithDataDto
import ru.citeck.ecos.process.domain.procdef.repo.*
import ru.citeck.ecos.process.domain.tenant.service.ProcTenantService
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records2.predicate.model.VoidPredicate
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

@Service
@RequiredArgsConstructor
class ProcDefServiceImpl(
    private val procDefRepo: ProcDefRepository,
    private val procDefRevRepo: ProcDefRevRepository,
    private val procStateRepo: ProcStateRepository,
    private val tenantService: ProcTenantService,
    private val recordsService: RecordsService
) : ProcDefService {

    companion object {
        private val STARTUP_TIME_STR = Instant.now().toEpochMilli().toString()
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
        }

        newRevision.processDef = currentProcDef
        newRevision = procDefRevRepo.save(newRevision)

        currentProcDef.lastRev = newRevision
        currentProcDef = procDefRepo.save(currentProcDef)

        return procDefToDto(currentProcDef)
    }

    override fun findAll(predicate: Predicate, max: Int, skip: Int): List<ProcDefDto> {
        return findAllProcDefEntities(predicate, max, skip)
            .map { entity: ProcDefEntity -> procDefToDto(entity) }
    }

    override fun findAllWithData(predicate: Predicate?, max: Int, skip: Int): List<ProcDefWithDataDto> {
        return findAllProcDefEntities(predicate ?: VoidPredicate.INSTANCE, max, skip).map { entity: ProcDefEntity ->
            val procDefDto = procDefToDto(entity)
            val procDefRevDto = procDefRevToDto(entity.lastRev!!)
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
                sectionRef = dto.sectionRef
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
                    sectionRef = dto.sectionRef
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

                result = procDefToDto(procDefRepo.save(procDefEntity))
            }
        }

        procDefListeners[result.procType]?.forEach { it.invoke(result) }

        return result
    }

    override fun getProcessDefRev(procType: String, procDefRevId: UUID): ProcDefRevDto? {
        val revId = EntityUuid(tenantService.getCurrent(), procDefRevId)
        val revEntity = procDefRevRepo.findById(revId).orElse(null) ?: return null
        return procDefRevToDto(revEntity)
    }

    override fun getProcessDefById(id: ProcDefRef): ProcDefWithDataDto? {
        val currentTenant = tenantService.getCurrent()
        val procDefEntity = procDefRepo.findFirstByIdTntAndProcTypeAndExtId(currentTenant, id.type, id.id)
        return procDefEntity?.let { def: ProcDefEntity ->
            ProcDefWithDataDto(procDefToDto(def), procDefRevToDto(def.lastRev!!))
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

        return processDef?.let { procDefRevToDto(it.lastRev!!) }
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

    private fun procDefToDto(entity: ProcDefEntity): ProcDefDto {
        val procDefDto = ProcDefDto()
        procDefDto.id = entity.extId
        procDefDto.procType = entity.procType
        procDefDto.name = mapper.read(entity.name, MLText::class.java)
        procDefDto.revisionId = entity.lastRev!!.id!!.id
        procDefDto.alfType = entity.alfType
        procDefDto.ecosTypeRef = RecordRef.valueOf(entity.ecosTypeRef)
        procDefDto.formRef = RecordRef.valueOf(entity.formRef)
        procDefDto.enabled = entity.enabled ?: false
        procDefDto.autoStartEnabled = entity.autoStartEnabled ?: false
        procDefDto.format = entity.lastRev?.format ?: ""
        procDefDto.sectionRef = EntityRef.valueOf(entity.sectionRef).ifEmpty {
            EntityRef.create(EprocApp.NAME, procDefDto.procType + "-section", "DEFAULT")
        }
        val entityCreated = entity.created ?: Instant.EPOCH
        val entityModified = entity.modified ?: Instant.EPOCH
        val lastRevCreated = entity.lastRev?.created ?: Instant.EPOCH
        procDefDto.created = entityCreated
        procDefDto.modified = lastRevCreated.coerceAtLeast(entityModified).coerceAtLeast(entityCreated)
        return procDefDto
    }

    private fun procDefRevToDto(entity: ProcDefRevEntity): ProcDefRevDto {
        val procDefRevDto = ProcDefRevDto()
        procDefRevDto.id = entity.id!!.id
        procDefRevDto.created = entity.created
        procDefRevDto.procDefId = entity.processDef!!.extId
        procDefRevDto.data = entity.data
        procDefRevDto.image = entity.image
        procDefRevDto.format = entity.format
        procDefRevDto.version = entity.version
        return procDefRevDto
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
