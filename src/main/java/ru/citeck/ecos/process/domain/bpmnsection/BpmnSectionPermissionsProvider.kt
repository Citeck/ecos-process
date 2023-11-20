package ru.citeck.ecos.process.domain.bpmnsection

import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.webapp.api.entity.EntityRef

@Component
class BpmnSectionPermissionsProvider(
    private val recordsService: RecordsService
) {

    fun hasPermissions(section: EntityRef, permission: BpmnPermission): Boolean {
        return recordsService.getAtt(section, permission.getAttribute()).asBoolean()
    }
}
