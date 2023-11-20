package ru.citeck.ecos.process.domain.bpmnsection.patch

import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.citeck.ecos.process.domain.bpmnsection.config.BpmnSectionConfig
import ru.citeck.ecos.records2.RecordConstants
import ru.citeck.ecos.records3.RecordsService
import ru.citeck.ecos.records3.record.atts.schema.annotation.AttName
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.webapp.api.constants.AppName
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatch
import ru.citeck.ecos.webapp.lib.patch.annotaion.EcosPatchDependsOnApps
import java.util.concurrent.Callable

@Component
@EcosPatchDependsOnApps(AppName.ALFRESCO)
@EcosPatch("bpmn-sections-alf-to-eproc-migration", "2022-07-12T00:00:00Z")
class AlfSectionsMigrationPatch(
    val recordsService: RecordsService
) : Callable<Any> {

    companion object {
        private val log = KotlinLogging.logger {}
        const val ALF_SOURCE_ID = AppName.ALFRESCO + "/"
        const val SPACES_STORE_PREFIX = "workspace://SpacesStore/"
    }

    override fun call(): Any {

        val resultMsgs = mutableListOf<String>()
        val msg = { msg: () -> String ->
            val value = msg.invoke()
            log.info { value }
            resultMsgs.add(value)
        }

        val alfQuery = RecordsQuery.create {
            withSourceId(ALF_SOURCE_ID)
            withLanguage("children")
            withQuery(
                mapOf(
                    "parent" to "${SPACES_STORE_PREFIX}ecos-bpm-category-root",
                    "assocName" to "cm:subcategories"
                )
            )
            withMaxItems(1000)
        }
        val alfSections = recordsService.query(alfQuery, AlfSectionAtts::class.java).getRecords()
            .filter {
                it.id != "${SPACES_STORE_PREFIX}cat-doc-kind-ecos-bpm-default"
            }

        if (alfSections.isEmpty()) {
            msg { "Alfresco sections is empty" }
            return resultMsgs
        } else {
            msg { "Found " + alfSections.size + " sections: " }
            alfSections.forEach {
                msg { "${it.id} - ${it.title}" }
            }
        }

        alfSections.map {
            val id = it.id.replaceFirst(SPACES_STORE_PREFIX, "")
            recordsService.create(
                BpmnSectionConfig.SOURCE_ID,
                mapOf(
                    "id" to id,
                    "name" to it.title
                )
            )
        }

        log.info { "Patch completed" }

        return resultMsgs
    }

    private class AlfSectionAtts(
        @AttName(RecordConstants.ATT_LOCAL_ID)
        val id: String,
        @AttName("cm:title")
        val title: String
    )
}
