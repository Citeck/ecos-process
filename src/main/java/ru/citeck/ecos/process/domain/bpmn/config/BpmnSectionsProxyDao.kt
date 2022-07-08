package ru.citeck.ecos.process.domain.bpmn.config

import org.springframework.stereotype.Component
import ru.citeck.ecos.commons.data.DataValue
import ru.citeck.ecos.commons.data.ObjectData
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
import ru.citeck.ecos.webapp.api.context.EcosWebAppContext

@Component
class BpmnSectionsProxyDao(
    private val ecosWebAppContext: EcosWebAppContext,
) : RecordsDaoProxy(
    id = SOURCE_ID,
    targetId = TARGET_SOURCE_ID
) {

    companion object {
        const val SOURCE_ID = "bpmn-section"
        const val TARGET_SOURCE_ID = BpmnSectionConfig.BPMN_SECTION_REPO_SOURCE_ID
    }

    private val alfSourceId = "alfresco/"
    private val alfIdPrefix = "alfresco\$"
    private val sourceIdMapping = mapOf(TARGET_SOURCE_ID to SOURCE_ID)
    private val querySectionsFromAlf = RecordsQuery.create {
        withSourceId(alfSourceId)
        withLanguage("children")
        withQuery(
            mapOf(
                "parent" to "workspace://SpacesStore/ecos-bpm-category-root",
                "assocName" to "cm:subcategories"
            )
        )
    }

    override fun queryRecords(recsQuery: RecordsQuery): RecsQueryRes<*>? {
        val targetQuery = recsQuery.copy().withSourceId(getTargetId()).build()

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

        val attsForEproc = getContextAtts(false)
        val recordAttsFromEproc = withSourceIdMapping {
            recordsService.getAtts(eprocRecords, attsForEproc)
        }
        val attsForAlfresco = getContextAtts(true)
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
            if (it.startsWith(alfIdPrefix)) {
                val alfRecordId = it.substring(9)
                alfRecords.add(toTargetRef(alfSourceId, alfRecordId))
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
        return listOf(DelStatus.OK)
    }

    override fun mutate(records: List<LocalRecordAtts>): List<String> {
        val eprocRecordAtts = mutableListOf<LocalRecordAtts>()
        val alfRecordAtts = mutableListOf<LocalRecordAtts>()
        splitRecordAtts(records, eprocRecordAtts, alfRecordAtts)

        val processedAlfRecordAtts = alfRecordAttsPreProcess(alfRecordAtts)

        val eprocResultRecordAtts = super.mutateWithoutProcessing(eprocRecordAtts)
        val alfResultRecordAtts = recordsService.mutate(processedAlfRecordAtts)

        val result = eprocResultRecordAtts + alfResultRecordAtts
        return result.map { it.id }
    }

    private fun splitRecordAtts(recordAtts: List<LocalRecordAtts>,
                                eprocRecordAtts: MutableList<LocalRecordAtts>,
                                alfRecordAtts: MutableList<LocalRecordAtts>) {
        recordAtts.forEach {
            val id = if (it.id.isNotEmpty()) it.id else it.attributes.get("id").asText()

            if (id.startsWith(alfIdPrefix)) {
                alfRecordAtts.add(it.withId(id.substring(9)))
            } else {
                eprocRecordAtts.add(it)
            }
        }
    }

    private fun alfRecordAttsPreProcess(alfRecordAtts: List<LocalRecordAtts>): List<RecordAtts> {
        return alfRecordAtts.map {
            val data = ObjectData.create()
            it.attributes.fieldNamesList().forEach { fieldName ->
                val value = it.attributes.get(fieldName)
                if (fieldName == "name") {
                    data["cm:title"] = value
                } else {
                    data[fieldName] = value
                }
            }
            RecordAtts(toTargetRef(alfSourceId, it.id), data)
        }
    }

    private fun getContextAtts(isAlfresco: Boolean): Map<String, String> {
        var schemaAtts = AttSchemaUtils.simplifySchema(AttContext.getCurrentSchemaAtt().inner)

        if (isAlfresco) {
            schemaAtts = attsPreProcess(schemaAtts)
        }

        val writer = serviceFactory.attSchemaWriter
        val result = LinkedHashMap<String, String>()

        schemaAtts.forEach { att ->
            result[att.getAliasForValue()] = writer.write(att)
        }

        return result
    }

    private fun attsPreProcess(schemaAtts: List<SchemaAtt>): List<SchemaAtt> {
        return schemaAtts.map {
            if (it.name == "name") {
                it.copy()
                    .withAlias(it.getAliasForValue())
                    .withName("cm:title")
                    .build()
            } else if (it.name == "parentId") {
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
            } else {
                it
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
        return ecosWebAppContext.getWebAppsApi().isAppAvailable("alfresco")
    }

    private fun replaceRecordsIdForAlfRecords(recsQueryRes: RecsQueryRes<RecordRef>) {
        recsQueryRes.setRecords(
            recsQueryRes.getRecords().map {
                RecordRef.create(getId(), alfIdPrefix + it.id)
            }
        )
    }

    private fun postProcessAlfAtts(recordAtts: List<RecordAtts>) {
        recordAtts.forEach {
            addAlfPrefixToParentIdAttValue(it)
        }
    }

    private fun addAlfPrefixToParentIdAttValue(recordAtts: RecordAtts) {
        val parentId = recordAtts.getAtt("parentId")
        if (parentId.isNotEmpty()) {
            val newParentId = DataValue.create(alfIdPrefix + parentId.asText())
            recordAtts.setAtt("parentId", newParentId)
        }
    }

    private fun toTargetRef(sourceId: String, recordId: String): RecordRef {
        return RecordRef.valueOf("$sourceId@$recordId")
    }
}
