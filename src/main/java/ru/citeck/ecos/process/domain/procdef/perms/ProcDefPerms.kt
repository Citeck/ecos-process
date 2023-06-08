package ru.citeck.ecos.process.domain.procdef.perms

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.permissions.service.RecordPermsService
import ru.citeck.ecos.model.lib.permissions.service.roles.RolesPermissions
import ru.citeck.ecos.model.lib.role.service.RoleService
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.ScalarType
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import javax.annotation.PostConstruct

private const val PERMS_READ = "Read"
private const val PERMS_WRITE = "Write"
private const val PERMS_DEPLOY = "deploy"

@Component
class ProcDefPermsServiceProvider(
    val recordPermsService: RecordPermsService,
    val roleService: RoleService,
    val recordsService: RecordsService
) {

    @PostConstruct
    private fun init() {
        srv = this
    }
}

private lateinit var srv: ProcDefPermsServiceProvider

class ProcDefPermsValue(
    private val recordRef: EntityRef
) : AttValue {

    private val recordPerms: RolesPermissions? by lazy {
        srv.recordPermsService.getRecordPerms(recordRef)
    }

    private val currentUserRoles by lazy {
        val typeRef = srv.recordsService.getAtt(
            recordRef,
            RecordConstants.ATT_TYPE + ScalarType.ID_SCHEMA
        ).toEntityRef()
        srv.roleService.getRolesId(typeRef)
            .filter { srv.roleService.isRoleMember(recordRef, it) }
            .toList()
    }

    override fun has(name: String): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }

        val perms = recordPerms ?: return false

        if (name.equals(PERMS_READ, true)) {
            return perms.isReadAllowed(currentUserRoles)
        }

        if (name.equals(PERMS_WRITE, true)) {
            return perms.isWriteAllowed(currentUserRoles)
        }

        return perms.isAllowed(currentUserRoles, name)
    }

    fun hasReadPerms(): Boolean {
        return has(PERMS_READ)
    }

    fun hasWritePerms(): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }

        // TODO: its temporary solution, need to fix it after https://citeck.atlassian.net/browse/ECOSCOM-5138
        if (isNewRecord(recordRef) && AuthContext.isRunAsAdmin()) {
            return true
        }

        return has(PERMS_WRITE)
    }

    fun hasDeployPerms(): Boolean {
        return has(PERMS_DEPLOY)
    }

    private fun isNewRecord(recordRef: EntityRef): Boolean {
        return recordRef.getLocalId().isEmpty()
    }
}
