package ru.citeck.ecos.process.common.section.perms

import ru.citeck.ecos.context.lib.auth.AuthRole
import ru.citeck.ecos.webapp.lib.perms.RecordPermsContext
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsComponent
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsData
import ru.citeck.ecos.webapp.lib.perms.component.RecordReadersPermsComponent
import ru.citeck.ecos.webapp.lib.perms.component.RecordReadersPermsData

class RootSectionPermsComponent : RecordPermsComponent, RecordReadersPermsComponent {

    override fun getRecordPerms(context: RecordPermsContext): RecordPermsData? {
        if (!isAcceptable(context)) {
            return null
        }
        return RootPerms(
            context.getAuthorities().contains(AuthRole.ADMIN),
            context.getAssignableAdditionalPerms()
        )
    }

    override fun getRecordReadersPerms(context: RecordPermsContext): RecordReadersPermsData? {
        if (!isAcceptable(context)) {
            return null
        }
        return RootReaders
    }

    private fun isAcceptable(context: RecordPermsContext): Boolean {
        return context.getRecord().getRef().getLocalId() == "ROOT"
    }

    override fun getOrder(): Float {
        return -10_000f
    }

    private object RootReaders : RecordReadersPermsData {

        override fun getAuthoritiesWithReadPermission(): Set<String> {
            return setOf(AuthRole.ADMIN)
        }

        override fun isFinalRecordReadersPermsData(): Boolean {
            return true
        }
    }

    private class RootPerms(
        private val isAdmin: Boolean,
        private val additionalPerms: Set<String>
    ) : RecordPermsData {

        override fun getAdditionalPerms(): Set<String> {
            return additionalPerms
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
