package ru.citeck.ecos.process.domain.bpmn.api.records

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmn.service.BpmnProcessService
import ru.citeck.ecos.process.domain.bpmn.service.CalledProcessInstanceMeta
import ru.citeck.ecos.process.domain.bpmn.service.isAllowForProcessInstanceId
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records2.predicate.PredicateUtils
import ru.citeck.ecos.records2.predicate.model.Predicate
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.query.RecordsQueryDao
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnCalledProcessInstanceRecords(
    private val bpmnProcessService: BpmnProcessService
) : AbstractRecordsDao(), RecordsQueryDao {

    companion object {
        const val ID = "bpmn-called-process-instance"
    }

    override fun getId() = ID

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*> {
        val predicate = recsQuery.getQuery(Predicate::class.java)
        val query = PredicateUtils.convertToDto(predicate, BpmnCalledProcessInstanceQuery::class.java)
        val processId = query.bpmnProcess.getLocalId()
        check(processId.isNotBlank()) {
            "Process id must be specified"
        }

        if (!BpmnPermission.PROC_INSTANCE_READ.isAllowForProcessInstanceId(processId)) {
            return RecsQueryRes<Any>()
        }

        val instances = bpmnProcessService.getCalledProcessInstancesMeta(processId)
            .map { CalledProcessInstanceRecord(it) }

        val result = RecsQueryRes<CalledProcessInstanceRecord>()
        result.setRecords(instances)
        result.setTotalCount(instances.size.toLong())

        return result
    }

    private inner class CalledProcessInstanceRecord(
        private val instanceMeta: CalledProcessInstanceMeta,

        val id: String = instanceMeta.id,
    ) {

        @AttName("incidents")
        fun getIncidents(): List<EntityRef> {
            return bpmnProcessService.getIncidentsByProcessInstanceId(id).map {
                EntityRef.create(AppName.EPROC, BpmnIncidentRecords.ID, it.id)
            }
        }

        @AttName("calledProcess")
        fun getCalledProcess(): EntityRef {
            return EntityRef.create(AppName.EPROC, BpmnProcessRecords.ID, id)
        }

        @AttName("bpmnDefEngine")
        fun getBpmnDefEngine(): EntityRef {
            return BpmnProcessDefEngineRecords.createRef(instanceMeta.processDefinitionId)
        }

        @AttName("callActivityId")
        fun getCallActivityId(): String {
            return instanceMeta.callActivityId
        }

        @AttName("isSuspended")
        fun isSuspended(): Boolean {
            val instance = bpmnProcessService.getProcessInstance(id)
            return instance?.isSuspended ?: false
        }
    }

    data class BpmnCalledProcessInstanceQuery(
        var bpmnProcess: EntityRef = EntityRef.EMPTY
    )
}
