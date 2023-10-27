package ru.citeck.ecos.process.common.section

import ru.citeck.ecos.process.domain.bpmnsection.dto.BpmnPermission
import ru.citeck.ecos.process.domain.dmnsection.dto.DmnPermission
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

enum class SectionType(
    val sourceId: String,
    val editInSectionPermissionId: String,
    val deployPermissionId: String,
    val createSectionPermissionId: String
) {
    BPMN(
        "bpmn-section",
        BpmnPermission.SECTION_EDIT_PROC_DEF.id,
        BpmnPermission.PROC_DEF_DEPLOY.id,
        BpmnPermission.SECTION_CREATE_SUBSECTION.id
    ),
    DMN(
        "dmn-section",
        DmnPermission.SECTION_EDIT_DMN_DEF.id,
        DmnPermission.DMN_DEF_DEPLOY.id,
        DmnPermission.SECTION_CREATE_SUBSECTION.id
    );

    fun getRef(id: String): EntityRef {
        return EntityRef.create(AppName.EPROC, sourceId, id)
    }
}
