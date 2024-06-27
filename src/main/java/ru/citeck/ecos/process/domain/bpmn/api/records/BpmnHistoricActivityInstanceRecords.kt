package ru.citeck.ecos.process.domain.bpmn.api.records

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.history.HistoricActivityInstance
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.process.domain.bpmn.service.BpmnPermissionResolver
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.AbstractRecordsDao
import ru.citeck.ecos.records3.record.dao.atts.RecordAttsDao

@Component
class BpmnHistoricActivityInstanceRecords(
    private val camundaHistoryService: HistoryService,
    private val bpmnPermissionResolver: BpmnPermissionResolver
) : AbstractRecordsDao(), RecordAttsDao {

    companion object {
        const val ID = "bpmn-historic-activity-instance"
    }

    override fun getId() = ID

    override fun getRecordAtts(recordId: String): Any? {
        val activityInstance: HistoricActivityInstance = camundaHistoryService.createHistoricActivityInstanceQuery()
            .activityInstanceId(recordId)
            .singleResult() ?: error("Activity instance with id $recordId not found")

        check(
            bpmnPermissionResolver.isAllowForProcessInstanceId(
                BpmnPermission.PROC_INSTANCE_READ,
                activityInstance.processInstanceId
            )
        ) {
            "User has no permission to read process instance ${activityInstance.processInstanceId}"
        }

        return ActivityInstanceRecord(activityInstance)
    }

    private inner class ActivityInstanceRecord(
        private val historicActivityInstance: HistoricActivityInstance,

        var id: String = historicActivityInstance.id
    ) {

        @AttName(".disp")
        fun getDisp(): MLText {
            return MLText(historicActivityInstance.activityName ?: "")
        }

        @AttName("activityId")
        fun getActivityId(): String {
            return historicActivityInstance.activityId
        }
    }
}
