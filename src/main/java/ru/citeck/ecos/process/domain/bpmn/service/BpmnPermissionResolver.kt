package ru.citeck.ecos.process.domain.bpmn.service

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef
import javax.annotation.PostConstruct

@Component
class BpmnPermissionResolver(
    val recordsService: RecordsService,
    val bpmnProcessDefFinder: BpmnProcessDefFinder
) {

    @PostConstruct
    private fun init() {
        bpmnPermissionResolver = this
    }
}

private lateinit var bpmnPermissionResolver: BpmnPermissionResolver

fun BpmnPermission.isAllow(bpmnProcessDef: EntityRef): Boolean {
    if (AuthContext.isRunAsSystemOrAdmin()) {
        return true
    }

    return bpmnPermissionResolver.recordsService.getAtt(
        bpmnProcessDef,
        getAttribute()
    ).asBoolean()
}

fun BpmnPermission.isAllowForBpmnDefEngine(bpmnDefEngine: EntityRef): Boolean {
    if (AuthContext.isRunAsSystemOrAdmin()) {
        return true
    }

    val bpmnProcessDef = bpmnPermissionResolver.bpmnProcessDefFinder.getByBpmnDefEngine(bpmnDefEngine)
    return isAllow(bpmnProcessDef)
}

fun BpmnPermission.isAllowForProcessInstanceId(processInstanceId: String): Boolean {
    if (AuthContext.isRunAsSystemOrAdmin()) {
        return true
    }

    val bpmnProcessDef = bpmnPermissionResolver.bpmnProcessDefFinder.getByProcessInstanceId(processInstanceId)
    return isAllow(bpmnProcessDef)
}

fun BpmnPermission.isAllowForDeploymentId(deploymentId: String): Boolean {
    if (AuthContext.isRunAsSystemOrAdmin()) {
        return true
    }

    val bpmnProcessDef = bpmnPermissionResolver.bpmnProcessDefFinder.getByDeploymentId(deploymentId)
    return isAllow(bpmnProcessDef)
}

fun BpmnPermission.isAllowForProcessKey(processKey: String): Boolean {
    if (AuthContext.isRunAsSystemOrAdmin()) {
        return true
    }

    val bpmnProcessDef = bpmnPermissionResolver.bpmnProcessDefFinder.getByProcessKey(processKey)
    return isAllow(bpmnProcessDef)
}
