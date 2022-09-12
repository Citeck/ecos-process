package ru.citeck.ecos.process.domain

import org.springframework.stereotype.Component
import org.springframework.util.ResourceUtils
import ru.citeck.ecos.commons.data.MLText
import ru.citeck.ecos.context.lib.auth.AuthContext
import ru.citeck.ecos.model.lib.type.service.utils.TypeUtils
import ru.citeck.ecos.process.domain.bpmn.BPMN_PROC_TYPE
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefActions
import ru.citeck.ecos.process.domain.bpmn.api.records.BpmnProcDefRecords
import ru.citeck.ecos.process.domain.proc.dto.NewProcessDefDto
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct

private val typeRef = TypeUtils.getTypeRef("type0")

/**
 * @author Roman Makarskiy
 */
@Component
class BpmnProcHelper(
    val recordsService: RecordsService
) {
    @PostConstruct
    private fun init() {
        helper = this
    }
}

private lateinit var helper: BpmnProcHelper

fun getBpmnProcessDefDto(resource: String, id: String): NewProcessDefDto {
    return NewProcessDefDto(
        id,
        MLText.EMPTY,
        BPMN_PROC_TYPE,
        "xml",
        "{http://www.citeck.ru/model/content/idocs/1.0}type",
        typeRef,
        EntityRef.EMPTY,
        ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)
            .toByteArray(StandardCharsets.UTF_8),
        null,
        enabled = true,
        autoStartEnabled = false,
        sectionRef = EntityRef.EMPTY
    )
}

fun saveAndDeployBpmnFromResource(resource: String, id: String) {
    val recordAtts = RecordAtts(RecordRef.create(AppName.EPROC, BpmnProcDefRecords.SOURCE_ID, "")).apply {
        this["processDefId"] = id
        this["definition"] = ResourceUtils.getFile("classpath:$resource")
            .readText(StandardCharsets.UTF_8)
        this["action"] = BpmnProcDefActions.DEPLOY.toString()
    }

    AuthContext.runAsSystem {
        helper.recordsService.mutate(recordAtts)
    }
}

fun saveAndDeployBpmn(elementFolder: String, id: String) {
    saveAndDeployBpmnFromResource(
        "test/bpmn/elements/$elementFolder/$id.bpmn.xml", id
    )
}
