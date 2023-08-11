package ru.citeck.ecos.process.domain.procdef.convert

import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.commons.json.Json
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefDto
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDataState
import ru.citeck.ecos.process.domain.procdef.dto.ProcDefRevDto
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefEntity
import ru.citeck.ecos.process.domain.procdef.repo.ProcDefRevEntity
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.ifEmpty
import java.time.Instant

fun ProcDefRevEntity.toDto(): ProcDefRevDto {
    return ProcDefRevDto(
        id = id!!.id,
        format = format!!,
        image = image,
        procDefId = processDef!!.extId!!,
        created = created,
        createdBy = createdBy,
        deploymentId = deploymentId,
        dataState = ProcDefRevDataState.from(dataState),
        version = version,
        initialData = data
    )
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
    procDefDto.dataState = ProcDefRevDataState.from(lastRev?.dataState)
    val entityCreated = created ?: Instant.EPOCH
    val entityModified = modified ?: Instant.EPOCH
    val lastRevCreated = lastRev?.created ?: Instant.EPOCH
    procDefDto.created = entityCreated
    procDefDto.modified = lastRevCreated.coerceAtLeast(entityModified).coerceAtLeast(entityCreated)
    return procDefDto
}
