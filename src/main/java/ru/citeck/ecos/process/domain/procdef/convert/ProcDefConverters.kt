package ru.citeck.ecos.process.domain.procdef.convert

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import java.time.Instant

fun ProcDefRevEntity.toDto(): ProcDefRevDto {
    val procDefRevDto = ProcDefRevDto()
    procDefRevDto.id = id!!.id
    procDefRevDto.created = created
    procDefRevDto.createdBy = createdBy
    procDefRevDto.deploymentId = deploymentId
    procDefRevDto.procDefId = processDef!!.extId
    procDefRevDto.data = data
    procDefRevDto.image = image
    procDefRevDto.format = format
    procDefRevDto.version = version
    return procDefRevDto
}

fun ProcDefEntity.toDto(): ProcDefDto {
    val procDefDto = ProcDefDto()
    procDefDto.id = extId
    procDefDto.procType = procType
    procDefDto.name = Json.mapper.read(name, MLText::class.java)
    procDefDto.revisionId = lastRev!!.id!!.id
    procDefDto.version = lastRev!!.version
    procDefDto.alfType = alfType
    procDefDto.ecosTypeRef = RecordRef.valueOf(ecosTypeRef)
    procDefDto.formRef = RecordRef.valueOf(formRef)
    procDefDto.enabled = enabled ?: false
    procDefDto.autoStartEnabled = autoStartEnabled ?: false
    procDefDto.format = lastRev?.format ?: ""
    procDefDto.sectionRef = EntityRef.valueOf(sectionRef).ifEmpty {
        EntityRef.create(EprocApp.NAME, procDefDto.procType + "-section", "DEFAULT")
    }
    val entityCreated = created ?: Instant.EPOCH
    val entityModified = modified ?: Instant.EPOCH
    val lastRevCreated = lastRev?.created ?: Instant.EPOCH
    procDefDto.created = entityCreated
    procDefDto.modified = lastRevCreated.coerceAtLeast(entityModified).coerceAtLeast(entityCreated)
    return procDefDto
}