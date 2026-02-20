package ru.citeck.ecos.process.common.section.perms

import ru.citeck.ecos.process.common.section.SectionType
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import ru.citeck.ecos.webapp.lib.perms.RecordPerms
import ru.citeck.ecos.webapp.lib.perms.RecordPermsContext
import ru.citeck.ecos.webapp.lib.perms.calculator.RecordPermsCalculator
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsComponent
import ru.citeck.ecos.webapp.lib.perms.component.RecordPermsData

class SectionFallbackPermsComponent(
    private val sectionType: SectionType,
    private val sectionPermsCalculator: RecordPermsCalculator
) : RecordPermsComponent {

    override fun getRecordPerms(context: RecordPermsContext): RecordPermsData {
        val sectionRefStr = context.getRecord().getAtt("sectionRef?id").asText()
        val sectionRef = if (sectionRefStr.isBlank()) {
            EntityRef.create(AppName.EPROC, sectionType.sourceId, "DEFAULT")
        } else {
            EntityRef.valueOf(sectionRefStr)
        }
        val sectionPerms = sectionPermsCalculator.getPermissions(
            sectionRef,
            context.getUser(),
            context.getAuthoritiesWithUser()
        )
        return SectionPermsData(sectionPerms, sectionType.editInSectionPermissionId)
    }

    private class SectionPermsData(
        private val sectionPerms: RecordPerms,
        private val editInSectionPermissionId: String
    ) : RecordPermsData {

        override fun hasReadPerms(): Boolean {
            return sectionPerms.hasReadPerms()
        }

        override fun hasWritePerms(): Boolean {
            return sectionPerms.hasWritePerms() || sectionPerms.hasPermission(editInSectionPermissionId)
        }

        override fun getAdditionalPerms(): Set<String> {
            return sectionPerms.getAdditionalPerms()
        }
    }
}
