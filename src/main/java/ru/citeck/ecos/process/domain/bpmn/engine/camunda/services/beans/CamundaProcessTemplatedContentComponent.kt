package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import org.springframework.stereotype.Component
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.api.entity.EntityRef

private const val TEMPLATED_CONTENT_RECORDS_ID = "transformations/templated-content"
private const val TEMPLATE_SOURCE_ID = "template"

@Component("templatedContent")
class CamundaProcessTemplatedContentComponent(
    private val recordsService: RecordsService
) : CamundaProcessEngineService {

    override fun getKey() = "templatedContent"

    fun write(record: Any, template: Any) {
        writeImpl(record, template)
    }

    fun write(record: Any, template: Any, attribute: String) {
        writeImpl(record, template, attribute)
    }

    fun write(record: Any, template: Any, parentAtt: String, childType: String) {
        writeImpl(record, template, parentAtt, childType)
    }

    fun write(record: Any, template: Any, parentAtt: String, childType: String, attribute: String) {
        writeImpl(record, template, parentAtt, childType, attribute)
    }

    private fun writeImpl(
        record: Any,
        template: Any,
        parentAtt: String = "",
        childType: String = "",
        attribute: String = RecordConstants.ATT_CONTENT
    ) {
        require(record is String || record is EntityRef) { "Record must be a string or EntityRef" }
        require(template is String || template is EntityRef) { "Template must be a string or EntityRef" }

        val templateStr = template.toString()
        val templatedContentAtts = RecordAtts("$TEMPLATED_CONTENT_RECORDS_ID@")
        templatedContentAtts["record"] = record
        templatedContentAtts["template"] = templateStr.toNormalizeTemplateRef()
        templatedContentAtts["parentAtt"] = parentAtt
        templatedContentAtts["childType"] = childType
        templatedContentAtts["attribute"] = attribute

        recordsService.mutate(templatedContentAtts)
    }

    private fun String.toNormalizeTemplateRef(): EntityRef {
        var templateRef = EntityRef.valueOf(this)

        if (templateRef.getAppName().isBlank()) {
            templateRef = templateRef.withAppName(AppName.TRANSFORMATIONS)
        }

        if (templateRef.getSourceId().isBlank()) {
            templateRef = templateRef.withSourceId(TEMPLATE_SOURCE_ID)
        }

        return templateRef
    }
}
