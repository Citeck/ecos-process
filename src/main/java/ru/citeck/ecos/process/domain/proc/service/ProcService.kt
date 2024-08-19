package ru.citeck.ecos.process.domain.proc.service

import ru.citeck.ecos.process.domain.proc.dto.NewProcessInstanceDto
import ru.citeck.ecos.process.domain.proc.dto.ProcessInstanceDto
import ru.citeck.ecos.process.domain.proc.dto.ProcessStateDto
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.util.*

interface ProcService {

    fun createProcessInstance(recordRef: EntityRef, procDefRevId: UUID): NewProcessInstanceDto

    fun getInstanceById(id: UUID): ProcessInstanceDto?

    fun updateStateData(prevStateId: UUID, data: ByteArray): ProcessStateDto

    fun getProcStateByProcId(procId: UUID): ProcessStateDto?

    fun getProcStateByStateId(procStateId: UUID): ProcessStateDto?
}
