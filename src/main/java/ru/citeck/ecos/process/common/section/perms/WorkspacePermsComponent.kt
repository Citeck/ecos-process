package ru.citeck.ecos.process.common.section.perms

import ru.citeck.ecos.model.lib.workspace.WorkspaceService
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.process.common.section.records.SectionsProxyDao
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.process.domain.dmnsection.dto.DmnPermission
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.perms.RecordPermsContext
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsComponent
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsData

class WorkspacePermsComponent(
    private val workspaceService: WorkspaceService,
    sectionType: SectionType
) : RecordPermsComponent {

    companion object {
        private const val PERM_READ = "read"
        private const val PERM_WRITE = "write"

        private val BPMN_MANAGER_PERMISSIONS = BpmnPermission.entries.mapTo(LinkedHashSet()) { it.id }

        private val BPMN_MEMBER_PERMISSIONS = setOf(
            BpmnPermission.READ.id,
            BpmnPermission.PROC_INSTANCE_RUN.id,
            BpmnPermission.PROC_DEF_REPORT_VIEW.id,
            BpmnPermission.PROC_INSTANCE_READ.id
        )

        private val DMN_MANAGER_PERMISSIONS = DmnPermission.entries.mapTo(LinkedHashSet()) { it.id }

        private val DMN_MEMBER_PERMISSIONS = setOf(
            DmnPermission.READ.id
        )

        private val EMPTY_PERMS = WsPermsData(emptySet())
    }

    private val managerPermissions = when (sectionType) {
        SectionType.BPMN -> BPMN_MANAGER_PERMISSIONS
        SectionType.DMN -> DMN_MANAGER_PERMISSIONS
    }

    private val memberPermissions = when (sectionType) {
        SectionType.BPMN -> BPMN_MEMBER_PERMISSIONS
        SectionType.DMN -> DMN_MEMBER_PERMISSIONS
    }

    override fun getRecordPerms(context: RecordPermsContext): RecordPermsData? {
        val workspace = context.getRecord().getAtt("workspace").asText()
        if (workspaceService.isWorkspaceWithGlobalEntities(workspace)) {
            return null
        }
        val user = context.getUser()
        if (!workspaceService.isUserMemberOf(user, workspace)) {
            return EMPTY_PERMS
        }
        if (!isDefaultSection(context)) {
            return null
        }
        val permissions = if (workspaceService.isUserManagerOf(user, workspace)) {
            managerPermissions
        } else {
            memberPermissions
        }
        return WsPermsData(permissions)
    }

    private fun isDefaultSection(context: RecordPermsContext): Boolean {
        val sectionRefStr = context.getRecord().getAtt("sectionRef?id").asText()
        if (sectionRefStr.isBlank()) {
            return true
        }
        return EntityRef.valueOf(sectionRefStr).getLocalId() == SectionsProxyDao.SECTION_DEFAULT
    }

    private class WsPermsData(private val permissions: Set<String>) : RecordPermsData {
        override fun hasReadPerms(): Boolean = PERM_READ in permissions
        override fun hasWritePerms(): Boolean = PERM_WRITE in permissions
        override fun getAdditionalPerms(): Set<String> = permissions.filterNotTo(LinkedHashSet()) {
            it.equals(PERM_READ, true) ||
                it.equals(PERM_WRITE, true)
        }
        override fun isFinalRecordPermsData(): Boolean = true
    }
}
