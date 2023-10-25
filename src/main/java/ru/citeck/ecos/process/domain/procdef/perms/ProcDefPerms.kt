package ru.citeck.ecos.process.domain.procdef.perms

import org.springframework.stereotype.Component
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.permissions.dto.PermissionType
import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.value.AttValue
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.api.entity.toEntityRef
import ru.citeck.ecos.webapp.lib.perms.EcosPermissionsService
import ru.citeck.ecos.webapp.lib.perms.RecordPerms
import javax.annotation.PostConstruct

private const val PERMS_READ = "read"
private const val PERMS_WRITE = "write"

private const val ATT_SECTION_REF_ID = "sectionRef?id"

@Component
class ProcDefPermsServiceProvider(
    val ecosPermissionsService: EcosPermissionsService,
    val recordsService: RecordsService
) {

    @PostConstruct
    private fun init() {
        srv = this
    }
}

private lateinit var srv: ProcDefPermsServiceProvider

class ProcDefPermsValue(
    private val record: Any,
    private val sectionType: SectionType
) : AttValue {

    private val recordPerms: RecordPerms by lazy {
        srv.ecosPermissionsService.getPermissions(record)
    }

    private val sectionPerms: RecordPerms by lazy {
        var sectionRef = srv.recordsService.getAtt(record, ATT_SECTION_REF_ID).asText().toEntityRef()
        if (EntityRef.isEmpty(sectionRef)) {
            sectionRef = EntityRef.create(AppName.EPROC, sectionType.sourceId, "DEFAULT")
        }
        srv.ecosPermissionsService.getPermissions(sectionRef)
    }

    override fun has(name: String): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }
        var hasPermission = recordPerms.hasPermission(name)
        if (!hasPermission) {
            hasPermission = sectionPerms.hasPermission(name)
        }
        if (!hasPermission && name.equals(PermissionType.WRITE.name, true)) {
            hasPermission = sectionPerms.hasPermission(sectionType.editInSectionPermissionId)
        }
        return hasPermission
    }

    fun hasReadPerms(): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }
        return has(PERMS_READ)
    }

    fun hasWritePerms(): Boolean {
        if (AuthContext.isRunAsSystem()) {
            return true
        }
        return has(PERMS_WRITE)
    }

    fun hasDeployPerms(): Boolean {
        return has(sectionType.deployPermissionId)
    }
}
