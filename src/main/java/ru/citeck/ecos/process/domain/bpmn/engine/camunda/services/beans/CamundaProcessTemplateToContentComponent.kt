package ru.citeck.ecos.process.domain.bpmn.engine.camunda.services.beans

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.context.lib.auth.AuthContext.runAsSystem
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.webapp.api.content.EcosContentApi
import ru.citeck.ecos.webapp.api.content.EcosContentWriter

//TODO: refactor or delete
@Component("templateToContent")
class CamundaProcessTemplateToContentComponent(
    private val recordsService: RecordsService,
    private val ecosContentApi: EcosContentApi
) : CamundaProcessEngineService {

    companion object {
        private val log = KotlinLogging.logger {}
    }

    fun write(template: String, record: String) {

        val templateModel = recordsService.getAtt(template, "model?json")
            .asMap(String::class.java, Any::class.java)
        val filledModel = recordsService.getAtts(record, templateModel).getAtts()
            .asMap(String::class.java, Any::class.java)

        val documentName = recordsService.getAtt(record, ".disp").toString()

        val fillTemplateRec = RecordAtts("transformations/fill-template@")
        fillTemplateRec["templateRef"] = template
        fillTemplateRec["resultName"] = documentName
        fillTemplateRec["model"] = filledModel

        val fillTemplateResult = recordsService.mutateAndGetAtts(fillTemplateRec, TemplateProcessResult::class.java)
        if (fillTemplateResult.content == null || fillTemplateResult.content.isEmpty()) {
            log.error { "Template result content is empty. TemplateRef: $template" }
        }

        //hack for mimeType
        val mimeType = let {
            val type = fillTemplateResult.mimeType?.get("type").toString().replace("\"", "")
            val subtype = fillTemplateResult.mimeType?.get("subtype").toString().replace("\"", "")
            "$type/$subtype"
        }

        val tempFile = runAsSystem {
            ecosContentApi.uploadTempFile()
                .withMimeType(
                    mimeType
                )
                //hack double dots
                .withName(fillTemplateResult.name?.replace("..", ".") ?: "document")
                .writeContent { writer: EcosContentWriter ->
                    writer.writeBytes(fillTemplateResult.content!!)
                }
        }

        val saveContentAtts = RecordAtts(record)
        saveContentAtts[RecordConstants.ATT_CONTENT] = tempFile
        recordsService.mutate(saveContentAtts)
    }

    override fun getKey(): String {
        return "templateToContent"
    }

    data class TemplateProcessResult(
        val name: String? = null,
        val content: ByteArray? = null,

        @AttName("mimeType?json")
        val mimeType: DataValue? = null
    )

}
