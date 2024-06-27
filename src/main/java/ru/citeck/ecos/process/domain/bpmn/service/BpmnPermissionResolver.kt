package ru.citeck.ecos.process.domain.bpmn.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmn.process.BpmnProcessDefFinder
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnPermissionResolver(
    private val recordsService: RecordsService,
    private val bpmnProcessDefFinder: BpmnProcessDefFinder
) {

    fun isAllow(permission: BpmnPermission, bpmnProcessDef: EntityRef): Boolean {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return true
        }

        return recordsService.getAtt(
            bpmnProcessDef,
            permission.getAttribute()
        ).asBoolean()
    }

    fun isAllowForBpmnDefEngine(permission: BpmnPermission, bpmnDefEngine: EntityRef): Boolean {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return true
        }

        val bpmnProcessDef = bpmnProcessDefFinder.getByBpmnDefEngine(bpmnDefEngine)
        return isAllow(permission, bpmnProcessDef)
    }

    fun isAllowForProcessInstanceId(permission: BpmnPermission, processInstanceId: String): Boolean {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return true
        }

        val bpmnProcessDef = bpmnProcessDefFinder.getByProcessInstanceId(processInstanceId)
        return isAllow(permission, bpmnProcessDef)
    }

    fun isAllowForDeploymentId(permission: BpmnPermission, deploymentId: String): Boolean {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return true
        }

        val bpmnProcessDef = bpmnProcessDefFinder.getByDeploymentId(deploymentId)
        return isAllow(permission, bpmnProcessDef)
    }

    fun isAllowForProcessKey(permission: BpmnPermission, processKey: String): Boolean {
        if (AuthContext.isRunAsSystemOrAdmin()) {
            return true
        }

        val bpmnProcessDef = bpmnProcessDefFinder.getByProcessKey(processKey)
        return isAllow(permission, bpmnProcessDef)
    }
}
