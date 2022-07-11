package ru.citeck.ecos.process.domain.bpmn.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.ObjectData
import ru.citeck.ecos.process.EprocApp
import ru.citeck.ecos.records2.RecordRef
import ru.citeck.ecos.records3.record.atts.dto.LocalRecordAtts
import ru.citeck.ecos.records3.record.atts.dto.RecordAtts
import ru.citeck.ecos.records3.record.atts.schema.SchemaAtt
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttContext
import ru.citeck.ecos.records3.record.atts.schema.resolver.AttSchemaUtils
import ru.citeck.ecos.records3.record.dao.delete.DelStatus
import ru.citeck.ecos.records3.record.dao.impl.proxy.RecordsDaoProxy
import ru.citeck.ecos.records3.record.dao.query.dto.query.RecordsQuery
import ru.citeck.ecos.records3.record.dao.query.dto.res.RecsQueryRes
import ru.citeck.ecos.records3.record.request.RequestContext
import ru.citeck.ecos.webapp.api.apps.EcosWebAppsApi
import ru.citeck.ecos.webapp.api.constants.AppName

@Component
class BpmnSectionsProxyDao(
    private val ecosWebAppsApi: EcosWebAppsApi
) : RecordsDaoProxy(
    id = SOURCE_ID,
    targetId = TARGET_SOURCE_ID
) {

    companion object {
        const val SOURCE_ID = "bpmn-section"
        const val TARGET_SOURCE_ID = BpmnSectionConfig.BPMN_SECTION_REPO_SOURCE_ID

        const val ALF_SOURCE_ID = AppName.ALFRESCO + "/"
        const val ALF_ID_PREFIX = AppName.ALFRESCO + "$"
    }

    private val sourceIdMapping = mapOf(TARGET_SOURCE_ID to SOURCE_ID)

    private val querySectionsFromAlf = RecordsQuery.create {
        withSourceId(ALF_SOURCE_ID)
        withLanguage("children")
        withQuery(
            mapOf(
                "parent" to "workspace://SpacesStore/ecos-bpm-category-root",
                "assocName" to "cm:subcategories"
            )
        )
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {

        val targetQuery = recsQuery.copy()
            .withSourceId(getTargetId())
            .build()

        val result = withSourceIdMapping {
            recordsService.query(targetQuery)
        }

        if (isAlfrescoAvailable()) {
            val queryResFromAlfresco = recordsService.query(querySectionsFromAlf)
            replaceRecordsIdForAlfRecords(queryResFromAlfresco)
            result.merge(queryResFromAlfresco)
        }

        return result
    }

    override fun getRecordsAtts(recordsId: List<String>): List<*> {

        val eprocRecords = mutableListOf<RecordRef>()
        val alfRecords = mutableListOf<RecordRef>()
        splitRecordRefs(recordsId, eprocRecords, alfRecords)

        val attsForEproc = preProcessAttsBeforeLoad(false)
        val recordAttsFromEproc = withSourceIdMapping {
            recordsService.getAtts(eprocRecords, attsForEproc)
        }
        val attsForAlfresco = preProcessAttsBeforeLoad(true)
        val recordAttsFromAlf = recordsService.getAtts(alfRecords, attsForAlfresco)
        postProcessAlfAtts(recordAttsFromAlf)

        return recordAttsFromEproc + recordAttsFromAlf
    }

    private fun splitRecordRefs(
        recordsId: List<String>,
        eprocRecords: MutableList<RecordRef>,
        alfRecords: MutableList<RecordRef>
    ) {
        recordsId.forEach {
            if (it.startsWith(ALF_ID_PREFIX)) {
                val alfRecordId = it.substring(ALF_ID_PREFIX.length)
                alfRecords.add(toTargetRef(ALF_SOURCE_ID, alfRecordId))
            } else {
                eprocRecords.add(toTargetRef(getTargetId(), it))
            }
        }
    }

    override fun delete(recordsId: List<String>): List<DelStatus> {

        val eprocRecords = mutableListOf<RecordRef>()
        val alfRecords = mutableListOf<RecordRef>()
        splitRecordRefs(recordsId, eprocRecords, alfRecords)

        withSourceIdMapping {
            recordsService.delete(eprocRecords)
        }
        recordsService.delete(alfRecords)

        return recordsId.map { DelStatus.OK }
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {

        val eprocRecordAtts = mutableListOf<LocalRecordAtts>()
        val alfRecordAtts = mutableListOf<LocalRecordAtts>()

        splitRecordAtts(records, eprocRecordAtts, alfRecordAtts)

        val processedAlfRecordAtts = processAttsBeforeAlfrescoMutation(alfRecordAtts)

        val eprocResultRecordAtts = super.mutateWithoutProcessing(eprocRecordAtts)
        val alfResultRecordAtts = recordsService.mutate(processedAlfRecordAtts)

        val result = eprocResultRecordAtts + alfResultRecordAtts
        return result.map { it.id }
    }

    private fun splitRecordAtts(recordAtts: List<LocalRecordAtts>,
                                eprocRecordAtts: MutableList<LocalRecordAtts>,
                                alfRecordAtts: MutableList<LocalRecordAtts>) {

        recordAtts.forEach {
            val id = it.id.ifEmpty {
                it.attributes["id"].asText()
            }
            if (id.startsWith(ALF_ID_PREFIX)) {
                alfRecordAtts.add(it.withId(id.substring(ALF_ID_PREFIX.length)))
            } else {
                eprocRecordAtts.add(it)
            }
        }
    }

    private fun processAttsBeforeAlfrescoMutation(alfRecordAtts: List<LocalRecordAtts>): List<RecordAtts> {
        return alfRecordAtts.map {
            val data = ObjectData.create()
            it.attributes.fieldNamesList().forEach { fieldName ->
                val value = it.attributes[fieldName]
                if (fieldName == "name") {
                    data["cm:title"] = value
                } else {
                    data[fieldName] = value
                }
            }
            RecordAtts(toTargetRef(ALF_SOURCE_ID, it.id), data)
        }
    }

    private fun preProcessAttsBeforeLoad(isAlfresco: Boolean): Map<String, String> {

        var schemaAtts = AttSchemaUtils.simplifySchema(AttContext.getCurrentSchemaAtt().inner)

        if (isAlfresco) {
            schemaAtts = alfGetAttsPreProcess(schemaAtts)
        }

        val writer = serviceFactory.attSchemaWriter
        val result = LinkedHashMap<String, String>()

        schemaAtts.forEach { att ->
            result[att.getAliasForValue()] = writer.write(att)
        }

        return result
    }

    private fun alfGetAttsPreProcess(schemaAtts: List<SchemaAtt>): List<SchemaAtt> {
        return schemaAtts.map {
            when (it.name) {
                "name" -> {
                    it.copy()
                        .withAlias(it.getAliasForValue())
                        .withName("cm:title")
                        .build()
                }
                "parentRef" -> {
                    it.copy()
                        .withAlias(it.getAliasForValue())
                        .withName("_parent")
                        .withInner(
                            SchemaAtt.create().withName("_localId")
                                .withInner(
                                    SchemaAtt.create().withName("?disp")
                                )
                        )
                        .build()
                }
                else -> it
            }
        }
    }

    private inline fun <T> withSourceIdMapping(crossinline action: () -> T): T {
        return RequestContext.doWithCtx({
            it.withSourceIdMapping(sourceIdMapping)
        }) {
            action.invoke()
        }
    }

    private fun isAlfrescoAvailable(): Boolean {
        return ecosWebAppsApi.isAppAvailable(AppName.ALFRESCO)
    }

    private fun replaceRecordsIdForAlfRecords(recsQueryRes: RecsQueryRes<RecordRef>) {
        recsQueryRes.setRecords(
            recsQueryRes.getRecords().map {
                RecordRef.create(getId(), ALF_ID_PREFIX + it.id)
            }
        )
    }

    private fun postProcessAlfAtts(recordAtts: List<RecordAtts>) {
        recordAtts.forEach {
            addAlfPrefixToAlfRefs(it)
        }
    }

    private fun addAlfPrefixToAlfRefs(recordAtts: RecordAtts) {
        recordAtts.getAtts().fieldNamesList().forEach { name ->
            if (name.contains("parentRef")) {
                val ref = recordAtts.getAtt(name)
                if (ref.isTextual() && ref.isNotEmpty()) {
                    val localId = ref.asText().substringAfter("@")
                    val newRef = EprocApp.NAME + "/" + getId() + "@" + ALF_ID_PREFIX + localId
                    recordAtts.setAtt(name, newRef)
                }
            }
        }
    }

    private fun toTargetRef(sourceId: String, recordId: String): RecordRef {
        return RecordRef.valueOf("$sourceId@$recordId")
    }
}
