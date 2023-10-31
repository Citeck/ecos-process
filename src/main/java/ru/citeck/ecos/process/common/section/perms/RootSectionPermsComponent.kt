package ru.citeck.ecos.process.common.section.perms

import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.webapp.lib.perms.RecordPermsContext
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsComponent
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsData

class RootSectionPermsComponent : RecordPermsComponent {

    override fun getRecordPerms(context: RecordPermsContext): RecordPermsData? {
        if (context.getRecord().getRef().getLocalId() == "ROOT") {
            return RootPerms(
                context.getAuthorities().contains(AuthRole.ADMIN),
                context.getAssignableAdditionalPerms()
            )
        }
        return null
    }

    override fun getOrder(): Float {
        return -10_000f
    }

    private class RootPerms(val isAdmin: Boolean, private val additionalPerms: Set<String>) : RecordPermsData {

        override fun getAdditionalPerms(): Set<String> {
            return additionalPerms
        }

        override fun getAuthoritiesWithReadPermission(): Set<String> {
            return setOf(AuthRole.ADMIN)
        }

        override fun hasReadPerms(): Boolean {
            return isAdmin
        }

        override fun hasWritePerms(): Boolean {
            return isAdmin
        }

        override fun isFinalRecordPermsData(): Boolean {
            return true
        }
    }
}
