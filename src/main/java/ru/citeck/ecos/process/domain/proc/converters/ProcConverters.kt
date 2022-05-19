package ru.citeck.ecos.process.domain.proc.converters

import ru.citeck.ecos.process.domain.proc.dto.ProcessInstanceDto
import ru.citeck.ecos.process.domain.proc.dto.ProcessStateDto
import ru.citeck.ecos.process.domain.proc.repo.ProcessInstanceEntity
import ru.citeck.ecos.process.domain.proc.repo.ProcessStateEntity
import ru.citeck.ecos.records2.RecordRef

fun ProcessStateEntity.toDto(): ProcessStateDto {
    return ProcessStateDto(
        id = id!!.id,
        data = data!!,
        processId = process!!.id!!.id,
        created = created,
        version = version,
        procDefRevId = procDefRev!!.id!!.id
    )
}

fun ProcessInstanceEntity.toDto(): ProcessInstanceDto {
    return ProcessInstanceDto(
        id = id!!.id,
        procType = procType!!,
        recordRef = RecordRef.valueOf(recordRef),
        stateId = state!!.id!!.id,
        created = created!!,
        modified = modified!!
    )
}
