package ru.citeck.ecos.process.domain.bpmn.process.delayed

import ru.citeck.ecos.model.lib.utils.ModelUtils
import ru.citeck.ecos.webapp.api.entity.EntityRef

object BpmnDelayedStartCmdDesc {

    const val BPMN_DELAYED_START_CMD_TYPE = "bpmn-delayed-start-cmd"
    const val BPMN_DELAYED_START_CMD_SOURCE_ID = BPMN_DELAYED_START_CMD_TYPE
    val BPMN_DELAYED_START_CMD_TYPE_REF = ModelUtils.getTypeRef(BPMN_DELAYED_START_CMD_TYPE)

    const val ATT_PROCESS_ID = "processId"
    const val ATT_WORKSPACE = "workspace"
    const val ATT_BUSINESS_KEY = "businessKey"
    const val ATT_VARIABLES = "variables"
    const val ATT_RETRY_COUNT = "retryCount"
    const val ATT_NEXT_RETRY_TIME = "nextRetryTime"
    const val ATT_LAST_PROC_RUN_TIME = "lastProcRunTime"
    const val ATT_COMPLETED_AT = "completedAt"
    const val ATT_LAST_ATTEMPT_TIME = "lastAttemptTime"
    const val ATT_LAST_ERROR = "lastError"

    fun getRef(localId: String): EntityRef {
        return EntityRef.create(BPMN_DELAYED_START_CMD_SOURCE_ID, localId)
    }
}
